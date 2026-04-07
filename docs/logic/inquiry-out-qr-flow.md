# Inquiry Out QR — Business Flow

**Endpoint:** `POST /inquiry-out-qr`  
**Ref:** `/docs/api/Controller.md`

---

## Happy Path

1. **Receive & validate request**
   - Required headers: `Authorization: Bearer <JWT>`, `Device-ID`
   - Required body fields: `qr_string`

2. **Extract identity from JWT**
   - Decoded by `JwtAuthFilter` (RS256)
   - Resolve `userId`, `customerId`, `mobileNo` from token claims

3. **Resolve account number from DB**
   - Query `ACCOUNT` table: `findByCustomerId(customerId)`
   - If no account found → return error response

4. **Load customer security questions**
   - Query `CUSTOMER_SECURITY_QUESTIONS` JOIN `SECURITY_QUESTIONS` for this customer
   - Result: list of `{ id, description }` (e.g. SQ1–SQ3)
   - If none found → return error response

5. **Call m-smart QR info**
   - `POST /m-smart/lmps/qr/info`
   - Payload: `qr_string`
   - Response: parsed QR fields including `memberId` (`to_member`), `receiverId` (`to_account`), `txnAmount`, `txnCurrency`, etc.
   - On timeout (>10 s) or error → return error response

6. **Call m-smart inquiry-out**
   - `POST /m-smart/lmps/out/inquiry/register` with `"toType": "QR"`
   - Payload: `to_account` = `receiverId`, `to_member` = `memberId` (from step 5), customer context
   - On timeout (>10 s) or error → return error response

7. **Build final response**
   - Base: m-smart inquiry `data` object (`txnId`, `reference`, `feelist`, `accountname`, etc.)
   - Merge QR info fields (`txnAmount`, `txnCurrency`, `purposeOfTxn`, `name`, `city`, etc.) into response
   - Generate `x_nonce` (UUID) — required for the subsequent transfer request
   - Inject `questions[]` (from step 4) into the root response body
   - Shape: `{ status, data, x_nonce, questions[] }`

8. **Return response to mobile app**

---

## Error Paths

| Condition | Behavior |
|---|---|
| Missing / invalid JWT | 401 — handled by `JwtAuthFilter` |
| Missing `qr_string` | 400 — return validation error |
| No account found for customer in DB | 400 — return error |
| No security questions found for customer | 400 — return error |
| m-smart QR info timeout | 504 — return error |
| m-smart QR info returns error | Forward m-smart error code/message |
| m-smart inquiry-out timeout | 504 — return error |
| m-smart inquiry-out returns error | Forward m-smart error code/message |
