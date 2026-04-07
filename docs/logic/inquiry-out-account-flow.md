# Inquiry Out — Business Flow

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

3. **Resolve account number from DB**
   - Query `ACCOUNT` table: `findByCustomerId(customerId)`
   - If no account found → return error response

4. **Load customer security questions**
   - Query `CUSTOMER_SECURITY_QUESTIONS` JOIN `SECURITY_QUESTIONS` for this customer
   - Result: list of `{ id, description }` (e.g. SQ1–SQ3)
   - If none found → return error response

5. **Call m-smart inquiry-out**
   - `POST /m-smart/lmps/out/inquiry/register`
   - Payload: `to_account`, `to_member`, customer context
   - On timeout (>10 s) or error → return error response

6. **Build final response**
   - Base: m-smart `data` object (`txnId`, `reference`, `feelist`, `accountname`, etc.)
   - Generate `x_nonce` (UUID) — required for the subsequent transfer request
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
