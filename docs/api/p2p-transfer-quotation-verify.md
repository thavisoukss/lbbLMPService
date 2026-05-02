# POST /p2p/transfer-quotation-verify

Executes a P2P gold transfer after security question verification.
Must be called within 3 minutes of the corresponding `/p2p/inquiry` call.

## Auth
`Authorization: Bearer <access_token>` (RS256 JWT)

## Request

```json
{
  "ref": "uuid from /p2p/inquiry response",
  "first_question_id": "string",
  "first_answer": "string",
  "second_question_id": "string",
  "second_answer": "string",
  "third_question_id": "string",
  "third_answer": "string"
}
```

## Response — 200 Success

```json
{
  "status": "success",
  "data": {
    "transaction_id": "P2P2612300123456789",
    "slip_code": "SLP23456789",
    "tran_date": "2026-05-02 16:40:00",
    "dr_account_no": "1200175610002081",
    "dr_account_name": "John Doe",
    "dr_account_ccy": "LBI",
    "cr_account_no": "1200178310002096",
    "cr_account_name": "Jane Smith",
    "cr_account_ccy": "LBI",
    "gold_weight": 0.5000,
    "memo": "Test transfer",
    "fee": 0.00
  }
}
```

## Error Responses

| HTTP | code | Condition |
|------|------|-----------|
| 404 | `QuotationNotFound` | `ref` not found, already used, or belongs to a different user |
| 200 | `QuotationAlreadyUsed` | Quotation was already verified (status != PENDING) |
| 200 | `QuotationExpired` | More than 3 minutes since inquiry |
| 200 | `ER_FIRST_ANSWER_INVALID` | First security answer incorrect |
| 200 | `ER_SECOND_ANSWER_INVALID` | Second security answer incorrect |
| 200 | `ER_THIRD_ANSWER_INVALID` | Third security answer incorrect |
| 500 | `INTERNAL_ERROR` | CBS call or DB failure |

## Notes
- Quotation TTL is 3 minutes from inquiry time.
- `fee` is always `0.00` (P2P transfers are fee-free).
- CBS P2P transfer is currently a **mockup** — replace `ApiCoreBanking.p2pTransfer()` once CBS `/p2p/transfer` spec is confirmed.
