# Flow — POST /p2p/transfer-quotation-verify

## Summary

```
POST /p2p/transfer-quotation-verify
    │
    ├── [1]  JWT auth (JwtAuthFilter)
    ├── [2]  Extract customerId from JWT claims
    ├── [3]  Load P2P_QUOTATION by ref → 404 if not found
    ├── [4]  Ownership check: quotation.customerId == caller → 404 on mismatch
    ├── [5]  Status check: must be PENDING → BusinessException if not
    ├── [6]  Expiry check: now < expiredAt (3 min TTL) → BusinessException if expired
    ├── [7]  Bcrypt verify 3 security answers from CUSTOMER_SECURITY_QUESTIONS
    ├── [8]  CBS p2pTransfer() [MOCKUP] → returns transactionId + slipCode
    ├── [9]  Insert P2P_TRANSACTION row
    ├── [10] Update P2P_QUOTATION status → COMPLETED
    └── [11] HTTP 200 P2PTransferVerifyResponse
```

## State Machine — P2P_QUOTATION.STATUS

```
PENDING  →  COMPLETED  (on successful transfer)
PENDING  →  (expired, TTL 3 min — no state change, just rejected at verify time)
```

## Key Classes

| Class | Role |
|-------|------|
| `P2PController.transferQuotationVerify` | Entry point, request/response logging |
| `P2PServiceImpl.transferQuotationVerify` | Full business logic |
| `P2PQuotationRepository` | CRUD on P2P_QUOTATION |
| `P2PTransactionRepository` | Insert P2P_TRANSACTION |
| `ApiCoreBanking.p2pTransfer` | CBS call (MOCKUP — replace when spec confirmed) |
| `SecurityQuestionRepository.findAnswersByCustomerId` | Loads bcrypt hashes for verification |

## CBS Integration (MOCKUP)

`ApiCoreBanking.p2pTransfer()` is currently a mockup that returns a generated
transaction ID and slip code without making a real HTTP call. When the CBS
`/p2p/transfer` endpoint spec is confirmed, implement the real call there and
remove the mockup comment.

## DB Tables

| Table | Operation |
|-------|-----------|
| `P2P_QUOTATION` | SELECT by ref, UPDATE status to COMPLETED |
| `P2P_TRANSACTION` | INSERT new row with transfer result |
| `CUSTOMER_SECURITY_QUESTIONS` | SELECT bcrypt answers for caller |
