# Transfer Out Account — Business Flow

**Endpoint:** `POST /transfer-out-account-quotation-verify`  
**Ref:** `/docs/api/Controller.md`

Prerequisites: a successful `/inquiry-out-account` call that produced a valid `x_nonce` and a `WITHDRAW_TXN` row in `DEBIT_PENDING` status.

---

## Processing Flow

```mermaid
sequenceDiagram
    participant App as Mobile App
    participant LMPS as LMPS Service
    participant DB as Oracle DB
    participant MS as m-smart

    App->>LMPS: POST /transfer-out-account-quotation-verify<br/>{x_nonce, to_account, amount, purpose, security answers × 3}
    LMPS->>LMPS: Extract JWT claims (userId, customerId, mobileNo)

    LMPS->>DB: CUSTOMER_SECURITY_QUESTIONS → load stored answers (BCrypt hashed)
    LMPS->>LMPS: Verify all 3 security question answers (BCrypt)

    LMPS->>DB: WITHDRAW_TXN → find by NONCE (x_nonce)
    LMPS->>LMPS: Assert owner customerId matches JWT
    LMPS->>LMPS: Assert STATUS = 'DEBIT_PENDING'

    LMPS->>LMPS: Read toMember from WITHDRAW_TXN.REMARK
    LMPS->>LMPS: Deserialize FEE_LIST from WITHDRAW_TXN
    LMPS->>LMPS: calculateFee(feeList, amount, currencyCode)

    LMPS->>MS: POST /m-smart/lmps/out/transfer<br/>{txnId, amount, txnFee, toType=ACCOUNT, toAcctId=CR_ACCOUNT_NO, toMemberId}
    MS-->>LMPS: {responseCode, data: {txnId, cbsRefNo, txnAmount, txnFee, ...}}

    LMPS->>DB: UPDATE WITHDRAW_TXN<br/>STATUS=COMPLETED, AMOUNT, FEE_AMT, CORE_BANKING_REF

    LMPS-->>App: {transaction_id, slip_code, tran_date, total_amount, fee_amt, dr/cr accounts, provider_ref, purpose}
```

---

## Happy Path

1. **Receive & validate request**
   - Required headers: `Authorization: Bearer <JWT>`, `Device-ID`
   - Required body fields: `x_nonce`, `to_account`, `amount` (positive), `first_question_id`, `first_answer`, `second_question_id`, `second_answer`, `third_question_id`, `third_answer`
   - Optional body field: `purpose`

2. **Extract identity from JWT**
   - Decoded by `JwtAuthFilter` (RS256)
   - Resolve `userId`, `customerId`, `mobileNo` from token claims

3. **Verify security question answers**
   - Load all stored answers for `customerId` from `CUSTOMER_SECURITY_QUESTIONS`
   - Stored answers are BCrypt-hashed; verified via `BCryptPasswordEncoder.matches()`
   - Answer check is case-insensitive (BCrypt handles this)
   - Any wrong answer or unknown question ID → `BusinessException("ER_FIRST/SECOND/THIRD_ANSWER_INVALID")` immediately

4. **Load pending transaction by nonce**
   - `SELECT * FROM WITHDRAW_TXN WHERE NONCE = x_nonce`
   - Not found → `ResourceNotFoundException` (invalid or expired nonce)
   - `WITHDRAW_TXN.CUSTOMER_ID ≠ customerId` → `ResourceNotFoundException` (ownership mismatch — treated as invalid nonce)
   - `STATUS ≠ 'DEBIT_PENDING'` → `BusinessException("4001")` (already processed or cancelled)

5. **Resolve toMember and calculate fee**
   - `toMember` = `WITHDRAW_TXN.REMARK` (the recipient bank code stored at inquiry time)
   - Deserialize `WITHDRAW_TXN.FEE_LIST` (JSON) into `FeeList` object (see fee calculation below)
   - Compute `txnFee` from `FeeList` using `amount` and `WITHDRAW_TXN.CURRENCY_CODE`

6. **Execute m-smart transfer-out**
   - `POST /m-smart/lmps/out/transfer` with `"securityContext.channel": "MSMART"`
   - Key payload fields:
     - `txnId` = `WITHDRAW_TXN.TRANSACTION_ID`
     - `txnType` = `"LMPOTA"`
     - `txnAmount` = `request.amount`
     - `txnFee` = calculated fee from step 5
     - `txnCcy` = `WITHDRAW_TXN.CURRENCY_CODE`
     - `toType` = `"ACCOUNT"`
     - `toAcctId` = `WITHDRAW_TXN.CR_ACCOUNT_NO` (resolved at inquiry time — not re-read from request)
     - `toMemberId` = `toMember` from step 5
     - `fromAcctId` = `WITHDRAW_TXN.DR_ACCOUNT_NO`
     - `fromCif` = `WITHDRAW_TXN.DR_CIF`
     - `fromCustName` = `WITHDRAW_TXN.DR_ACCOUNT_NAME`
     - `toCustName` = `WITHDRAW_TXN.CR_ACCOUNT_NAME`
   - On timeout (>10 s) or non-`0000` response → return error

7. **Update `WITHDRAW_TXN`** *(within `@Transactional`)*
   - `STATUS` = `'COMPLETED'`
   - `AMOUNT` = `result.txnAmount` (fallback: `request.amount`)
   - `FEE_AMT` = `result.txnFee` (fallback: calculated fee from step 5)
   - `FEE_PROVIDER_AMT` = same as `FEE_AMT`
   - `CORE_BANKING_REF` = `result.cbsRefNo`

8. **Build and return response**
   - Shape: flat object (no wrapping `data` key)

   | Field | Source |
   |-------|--------|
   | `transaction_id` | `result.txnId` |
   | `slip_code` | `result.txnId` |
   | `tran_date` | `LocalDateTime.now()` formatted `yyyy-MM-dd HH:mm:ss` |
   | `total_amount` | `result.txnAmount` |
   | `currency_code` | `result.txnCcy` |
   | `fee_amt` | `result.txnFee` |
   | `dr_account_no` | `result.fromAcctId` |
   | `dr_account_name` | `result.fromCustName` |
   | `cr_account_no` | `result.toAcctId` |
   | `cr_account_name` | `result.toCustName` |
   | `provider_ref` | `result.cbsRefNo` |
   | `purpose` | `result.purpose` |

---

## Fee Calculation Logic

Fee tiers are stored as a JSON snapshot in `WITHDRAW_TXN.FEE_LIST` at inquiry time. At transfer time the fee is computed locally — no re-fetch from m-smart.

**Algorithm:** Walk tiers in ascending `from` order; the last tier where `amount >= from` applies.

**Example** (LAK, amount = 2,300 LAK):

| Tier `from` | `feeamount` | Applies? |
|-------------|-------------|---------|
| 0 | 1,000 | yes |
| 2,000,001 | 1,500 | no — 2,300 < 2,000,001 |

→ Fee = **1,000 LAK**

---

## Differences from Transfer Out QR

| Aspect | Account | QR |
|--------|---------|-----|
| `toAcctId` source | `WITHDRAW_TXN.CR_ACCOUNT_NO` | raw `qr_string` from request |
| `toMemberId` source | `WITHDRAW_TXN.REMARK` | m-smart QR info call |
| m-smart QR info call | No | Yes |
| Nonce ownership check | Yes — `CUSTOMER_ID` must match JWT | No |
| Security answer hashing | BCrypt | Plain equalsIgnoreCase |

---

## Error Paths

| Condition | Behavior |
|---|---|
| Missing / invalid JWT | 401 — handled by `JwtAuthFilter` |
| Missing or invalid required fields | 400 — bean validation error |
| Security question answer wrong or question ID not found | `BusinessException("ER_*_ANSWER_INVALID")` |
| `x_nonce` not found in `WITHDRAW_TXN` | `ResourceNotFoundException` |
| Nonce belongs to a different customer | `ResourceNotFoundException` |
| `WITHDRAW_TXN.STATUS ≠ 'DEBIT_PENDING'` | `BusinessException("4001")` |
| m-smart transfer-out timeout | 504 — return error |
| m-smart transfer-out returns non-`0000` response | Forward m-smart error code/message |
| `WITHDRAW_TXN` update fails | 500 — transaction rolled back; transfer may have already completed at m-smart |