# Inquiry Out Account — Business Flow

**Endpoint:** `POST /inquiry-out-account`  
**Ref:** `/docs/api/Controller.md`

---

## Processing Flow

```mermaid
sequenceDiagram
    participant App as Mobile App
    participant LMPS as LMPS Service
    participant DB as Oracle DB
    participant MS as m-smart

    App->>LMPS: POST /inquiry-out-account<br/>{to_account, to_member}
    LMPS->>LMPS: Extract JWT claims (userId, customerId, mobileNo)

    par Load customer context
        LMPS->>DB: ACCOUNT → find accountNo by customerId
        LMPS->>DB: CUSTOMER → resolve display name
        LMPS->>DB: SECURITY_QUESTIONS → load questions list
    end

    LMPS->>LMPS: Generate txnId (commonInfo.genTransactionId)
    LMPS->>MS: POST /m-smart/lmps/out/inquiry/register<br/>toType=ACCOUNT, txnId, to_account, to_member
    MS-->>LMPS: {responseCode, data: {txnId, accountname, feelist, accountccy, reference, ...}}

    LMPS->>LMPS: Generate x_nonce (UUID)
    LMPS->>LMPS: Serialize feelist → JSON string
    LMPS->>DB: INSERT WITHDRAW_TXN<br/>(STATUS=DEBIT_PENDING, AMOUNT=0, FEE_LIST=feelist JSON)

    LMPS-->>App: {status, data, x_nonce, questions[]}
```

---

## Happy Path

1. **Receive & validate request**
   - Required headers: `Authorization: Bearer <JWT>`, `Device-ID`
   - Required body fields: `to_account`, `to_member`

2. **Extract identity from JWT**
   - Decoded by `JwtAuthFilter` (RS256)
   - Resolve `userId`, `customerId`, `mobileNo` from token claims

3. **Load customer context from DB** *(3 queries)*
   - `ACCOUNT`: find active LAK account by `customerId` → `accountNo`
   - `CUSTOMER`: resolve display name — `NAME` if set, else `FIRST_NAME_EN + LAST_NAME_EN`, fallback to `userId`
   - `CUSTOMER_SECURITY_QUESTIONS` JOIN `SECURITY_QUESTIONS` → list of `{ id, description }`
   - No account found → return error response
   - No security questions found → return error response

4. **Generate txnId and call m-smart inquiry-out**
   - `txnId` is generated locally by this service via `commonInfo.genTransactionId("")` and included in the request to m-smart
   - `POST /m-smart/lmps/out/inquiry/register` with `"toType": "ACCOUNT"`, `"securityContext.channel": "MOBILE"`
   - Payload: generated `txnId`, `to_account`, `to_member`, sender `accountNo`, `customerId` (fromCif), `userId` (fromuser)
   - Response confirms the same `txnId` and returns: `accountname` (CR name), `feelist`, `accountccy`, `reference`, etc.
   - On timeout (>10 s) or non-`0000` response → return error

5. **Save inquiry record to `WITHDRAW_TXN`**
   - Generate `x_nonce` (UUID) — returned to client for subsequent transfer call
   - Insert one row:
     - `PAYMENT_CHANNEL_ID = 25` (Lao QR channel — hardcoded)
     - `STATUS = 'DEBIT_PENDING'`, `AMOUNT = 0`
     - `TRANSACTION_ID` = `txnId`
     - `NONCE` = `x_nonce`
     - `PROVIDER_CODE` = `'LMPS'`
     - `DR_ACCOUNT_NO` = sender account number; `DR_CIF` = `customerId`; `DR_ACCOUNT_NAME` = customer display name
     - `CR_ACCOUNT_NO` = `toaccount` from m-smart response
     - `CR_ACCOUNT_NAME` = `accountname` from m-smart response
     - `CURRENCY_CODE` = `accountccy` from inquiry response, default `'LAK'`
     - `REMARK` = `'ACCOUNT'`
     - `FEE_LIST` = `feelist` object from m-smart response serialized to JSON string (snapshot used for fee calculation at transfer time)
   - If save fails → request fails; no `x_nonce` is issued to the client

6. **Build final response**
   - Base: m-smart `data` object (`txnId`, `reference`, `feelist`, `accountname`, etc.)
   - Set `x_nonce` = UUID from step 5 (must match `WITHDRAW_TXN.NONCE`)
   - Inject `questions[]` (from step 3) into the root response body
   - Shape: `{ status, data, x_nonce, questions[] }`

7. **Return response to mobile app**

---

## Error Paths

| Condition | Behavior |
|---|---|
| Missing / invalid JWT | 401 — handled by `JwtAuthFilter` |
| Missing `to_account` or `to_member` | 400 — return validation error |
| No account found for customer in DB | 400 — return error |
| No security questions found for customer | 400 — return error |
| m-smart timeout | 504 — return error |
| m-smart returns non-`0000` response | Forward m-smart error code/message |
| `WITHDRAW_TXN` save fails | 500 — return error; no `x_nonce` issued |