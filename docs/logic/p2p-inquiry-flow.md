# P2P Inquiry — Business Flow

**Endpoint:** `POST /p2p/inquiry`  
**Ref:** `/docs/api/p2pController.md`

---

## Processing Flow

```mermaid
sequenceDiagram
    participant App as Mobile App
    participant LMPS as LMPS Service
    participant DB as Oracle DB
    participant CBS as Core Banking

    App->>LMPS: POST /p2p/inquiry<br/>{gold_weight, cr_phone, memo}
    LMPS->>LMPS: Extract customerId from JWT claims

    par Load DR and CR accounts
        LMPS->>DB: CUSTOMER → find debtor by customerId
        LMPS->>DB: ACCOUNT → find DR LBI CURRENT by customerId
        LMPS->>DB: CUSTOMER → find creditor by cr_phone
        LMPS->>DB: ACCOUNT → find CR LBI CURRENT by creditor.id
    end

    LMPS->>CBS: GET /gold-rate
    CBS-->>LMPS: {sellRate, buyRate, currency}

    LMPS->>DB: SECURITY_QUESTIONS JOIN CUSTOMER_SECURITY_QUESTIONS<br/>WHERE customerId AND STATUS='ACTIVE'
    DB-->>LMPS: [{id, description} × 3]

    LMPS->>LMPS: Compute total_amount = gold_weight × sellRate
    LMPS->>LMPS: Generate ref (UUID)

    LMPS-->>App: {status, data: {ref, ttl, dr/cr accounts, total_amount, questions[]}}
```

---

## Happy Path

1. **Receive request**
   - Required headers: `Authorization: Bearer <JWT>`, `Content-Type: application/json`
   - Required body fields: `gold_weight`, `cr_phone`; `memo` is optional

2. **Extract identity from JWT**
   - Decoded by `JwtAuthFilter` (RS256)
   - `customerId` is read from token claim `user-id`

3. **Load debtor (DR) account**
   - `CUSTOMER`: `findById(customerId)` → not found → 404
   - `ACCOUNT`: `findLbiCurrentByCustomerId(customerId)` → filter `ACCOUNT_CURRENCY='LBI'`, `ACCOUNT_TYPE='CURRENT'` → not found → 404

4. **Load creditor (CR) account**
   - `CUSTOMER`: `findByPhone(crPhone)` → not found → 404
   - `ACCOUNT`: `findLbiCurrentByCustomerId(creditor.id)` → same filter → not found → 404

5. **Fetch LBI gold rate from CBS**
   - `GET /gold-rate` via `ApiCoreBanking` (`RestClient`, connect 5 s / read 10 s timeout)
   - `total_amount = gold_weight × sellRate`
   - Error or timeout → 500

6. **Load security questions** *(1 JOIN query)*
   - `SECURITY_QUESTIONS JOIN CUSTOMER_SECURITY_QUESTIONS WHERE CUSTOMER_ID=:customerId AND STATUS='ACTIVE' AND DELETE_AT IS NULL`
   - Returns 3 rows `{id, description}`
   - Error → 500

7. **Build and return response**
   - Generate `ref = UUID.randomUUID()` — returned to the client as the transfer nonce
   - `ttl` = `180` s — client should not hold this quotation longer
   - DR and CR account fields from steps 3–4
   - `total_amount` = gold_weight × sellRate
   - `questions[]` = 3 entries from step 6

---

## Key Constants

| Name | Value | Source |
|---|---|---|
| CBS endpoint | `GET /gold-rate` | `ApiCoreBanking.getGoldRate()` |
| CBS timeout | connect 5 s / read 10 s | `RestClient` bean config |
| Response `ttl` | 180 s | `P2PServiceImpl` |
| Account filter | `ACCOUNT_CURRENCY='LBI'`, `ACCOUNT_TYPE='CURRENT'` | `AccountRepository` |
| `ref` | raw UUID (no prefix) | `P2PServiceImpl` |

---

## Error Paths

| Condition | Behavior |
|---|---|
| Missing / invalid JWT | 401 — handled by `JwtAuthFilter` |
| `customerId` claim missing | 401 |
| Debtor customer not found | 404 `AccountInfoNotFound` |
| Debtor LBI CURRENT account not found | 404 `AccountInfoNotFound` |
| Creditor customer not found (by phone) | 404 `AccountInfoNotFound` |
| Creditor LBI CURRENT account not found | 404 `AccountInfoNotFound` |
| CBS `/gold-rate` error / timeout | 500 |
| Security questions query error | 500 |
