# Flow — POST /p2p/transfer-quotation-verify

## Summary

```
POST /p2p/transfer-quotation-verify
    │
    ├── [1]  JWT auth (JwtAuthFilter)
    ├── [2]  Extract customerId from JWT claims
    ├── [3]  Load P2P_TXN_DETAIL by txn_id → 404 if not found
    ├── [4]  Ownership check: detail.customerId == caller → 404 on mismatch
    ├── [5]  Status check: must be PENDING → BusinessException if not
    ├── [6]  Expiry check: now < expiredAt (3 min TTL) → BusinessException if expired
    ├── [7]  Bcrypt verify 3 security answers from CUSTOMER_SECURITY_QUESTIONS
    ├── [8]  CBS p2pTransfer() [MOCKUP] → returns cbsRefNo (starts with 'CBS') + slipCode + drCbsSeqno + crCbsSeqno
    ├── [9]  Update P2P_TXN_DETAIL status → COMPLETED, set cbs_ref_no, dr_cbs_seqno, cr_cbs_seqno, update_at
    └── [10] HTTP 200 P2PTransferVerifyResponse
```

## State Machine — P2P_TXN_DETAIL.STATUS

```
PENDING  →  COMPLETED  (on successful transfer)
PENDING  →  (expired, TTL 3 min — no state change, just rejected at verify time)
```

## Key Classes

| Class | Role |
|-------|------|
| `P2PController.transferQuotationVerify` | Entry point, request/response logging |
| `P2PServiceImpl.transferQuotationVerify` | Full business logic |
| `P2PTxnDetailRepository` | CRUD on P2P_TXN_DETAIL |
| `ApiCoreBanking.p2pTransfer` | CBS call (MOCKUP — replace when spec confirmed) |
| `SecurityQuestionRepository.findAnswersByCustomerId` | Loads bcrypt hashes for verification |

## CBS Integration (MOCKUP)

`ApiCoreBanking.p2pTransfer()` is currently a mockup that returns generated values
without making a real HTTP call:

| Field | Mock format | Example |
|-------|------------|---------|
| `cbsRefNo` (= `CBS_REF_NO`) | `CBS` + last 8 chars of txnId | `CBS34398` |
| `slipCode` | `SLP` + last 8 chars of txnId | `SLP34398` |
| `drCbsSeqno` (= `DR_CBS_SEQNO`) | `DR` + last 8 chars of txnId | `DR34398` |
| `crCbsSeqno` (= `CR_CBS_SEQNO`) | `CR` + last 8 chars of txnId | `CR34398` |

When the CBS `/p2p/transfer` endpoint spec is confirmed, implement the real call in
`ApiCoreBanking.p2pTransfer()` and remove the mockup comment.

## DB Tables

| Table | Operation |
|-------|-----------|
| `P2P_TXN_DETAIL` | SELECT by txn_id; UPDATE status to COMPLETED + set `CBS_REF_NO`, `DR_CBS_SEQNO`, `CR_CBS_SEQNO`, `UPDATE_AT` |
| `CUSTOMER_SECURITY_QUESTIONS` | SELECT bcrypt answers for caller |
