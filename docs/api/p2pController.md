# P2P Controller — API Reference

**Base URL:** `http://<host>:8084/api/p2p`  
**Authentication:** All endpoints require `Authorization: Bearer <JWT>` (RS256)

---

## Endpoints

### POST /get-account-info-by-phone
Look up a P2P recipient's account details by phone number. Used by the mobile app before initiating a P2P transfer.

**Headers**

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | `Bearer <JWT>` |
| `Content-Type` | Yes | `application/json` |

**Request body**
```json
{
  "cr_phone": "02012345678"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `cr_phone` | string | Yes | Recipient phone number |

**Response `200 OK`**
```json
{
  "status": "success",
  "data": {
    "account_no": "1200182110002250",
    "account_name": "PITI-PHANTHASOMBATH",
    "account_currency": "LAK",
    "profile_image": "https://example.com/profile.jpg"
  }
}
```

**Response fields**

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"success"` on success |
| `data.account_no` | string | Recipient account number |
| `data.account_name` | string | Recipient account name |
| `data.account_currency` | string | Recipient account currency (e.g. `LAK`) |
| `data.profile_image` | string | Recipient profile image URL |

---

### POST /inquiry
Pre-transfer inquiry for a P2P (gold-weight) transfer. Returns a quotation including debit/credit account details, total amount, and security questions.

**Headers**

| Header | Required | Description |
|--------|----------|-------------|
| `Authorization` | Yes | `Bearer <JWT>` |
| `Content-Type` | Yes | `application/json` |

**Request body**
```json
{
  "gold_weight": 0.5,
  "cr_phone": "02012345678",
  "memo": "ຊ່ວຍຄ່າກິນເຂົ້າ"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `gold_weight` | number | Yes | Gold weight amount to transfer |
| `cr_phone` | string | Yes | Recipient phone number |
| `memo` | string | No | Transfer note/memo |

**Response `200 OK`**
```json
{
  "status": "success",
  "data": {
    "ref": "a3f1c2d4-e5b6-7890-abcd-ef1234567890",
    "ttl": 180,
    "dr_account_no": "1200182110002250",
    "dr_account_name": "PITI-PHANTHASOMBATH",
    "dr_account_currency": "LBI",
    "cr_account_no": "1200182110009999",
    "cr_account_name": "ANOUDETH XAYACHACK",
    "cr_account_currency": "LBI",
    "total_amount": 1500000,
    "gold_weight": 0.5,
    "memo": "ຊ່ວຍຄ່າກິນເຂົ້າ",
    "questions": [
      { "id": "SQ1", "description": "ວັນເກີດຂອງເເມ່ທ່ານເເມ່ນຫັຍງ?" },
      { "id": "SQ2", "description": "ວັນເກີດຂອງພໍ່ທ່ານເເມ່ນຫັຍງ?" },
      { "id": "SQ3", "description": "ເບີໂທເເມ່ທ່ານເເມ່ນຫັຍງ?" }
    ]
  }
}
```

**Response fields**

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | `"success"` on success |
| `data.ref` | string | Quotation reference UUID — passed to the transfer/confirm step |
| `data.ttl` | number | Time-to-live in seconds; client must not hold this quotation past `180` s |
| `data.dr_account_no` | string | Sender (debit) LBI account number |
| `data.dr_account_name` | string | Sender account name |
| `data.dr_account_currency` | string | Always `"LBI"` |
| `data.cr_account_no` | string | Recipient (credit) LBI account number |
| `data.cr_account_name` | string | Recipient account name |
| `data.cr_account_currency` | string | Always `"LBI"` |
| `data.total_amount` | number | Calculated amount (`gold_weight × CBS sell rate`) |
| `data.gold_weight` | number | Gold weight echoed back from request |
| `data.memo` | string | Transfer note echoed back from request |
| `data.questions` | array | Security questions the user must answer before confirming |
| `data.questions[].id` | string | Question ID (e.g. `SQ1`) |
| `data.questions[].description` | string | Question text in Lao |

**Error response**
```json
{
  "status": "error",
  "error": {
    "code": "ER_INTERNAL_SERVER_ERROR"
  },
  "message": "..."
}
```

See [errors.md](errors.md) for the full error response format.