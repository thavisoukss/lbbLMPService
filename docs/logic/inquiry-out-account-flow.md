# Inquiry Out Account — Business Flow

**Endpoint:** `POST /inquiry-out-account`  
**Ref:** `/docs/api/Controller.md`

---

## Happy Path

1. **Receive & validate request**
   - Required headers: `Authorization: Bearer <JWT>`, `Device-ID`
   - Required body fields: `to_account`, `to_member`

2. **Extract identity from JWT**
   - Decoded by `JwtAuthFilter` (RS256)
   - Resolve `userId`, `customerId`, `mobileNo` from token claims

3. **Load customer context from DB** *(3 queries, executed once)*
   - Query `ACCOUNT` table: find active LAK account by `customerId` → `accountNo`
   - Query `CUSTOMER` table: resolve display name — use `NAME` if set, otherwise `FIRST_NAME_EN + LAST_NAME_EN`, fallback to `userId`
   - Query `CUSTOMER_SECURITY_QUESTIONS` JOIN `SECURITY_QUESTIONS` → list of `{ id, description }`
   - If no account found → return error response
   - If no security questions found → return error response

4. **Call m-smart inquiry-out**
   - `POST /m-smart/lmps/out/inquiry/register` with `"toType": "ACCOUNT"`
   - Payload: `to_account`, `to_member`, customer context
   - Response: `txnId`, `accountname` (CR name), `feelist`, `accountccy`, etc.
   - On timeout (>10 s) or error → return error response

5. **Save inquiry record to `WITHDRAW_TXN`**
   - Insert one row with `STATUS = DEBIT_PENDING`, `AMOUNT = 0`
   - `TRANSACTION_ID` = `txnId` from m-smart inquiry response
   - `NONCE` = generated UUID (returned to client as `x_nonce`)
   - `DR_ACCOUNT_NO` = sender account number; `DR_CIF` = `customerId`; `DR_ACCOUNT_NAME` = customer display name
   - `CR_ACCOUNT_NO` = `toaccount` from m-smart inquiry response
   - `CR_ACCOUNT_NAME` = `accountname` from m-smart inquiry response
   - `CURRENCY_CODE` = `accountccy` from inquiry response, default `LAK`
   - `REMARK` = `"ACCOUNT"`
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
| m-smart returns error | Forward m-smart error code/message |
| `WITHDRAW_TXN` save fails | 500 — return error; no `x_nonce` issued |
