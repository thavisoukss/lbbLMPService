# CBS (Core Banking System) — External API

**Base URL (UAT):** `https://uatapiv2.laobullionbank.com:8899`  
**Config key:** `external.api.core-banking.url`  
**Client:** `ApiCoreBanking` (`RestClient`, connect 5 s / read 10 s)

---

## POST /api/corebanking/getRate

Fetch exchange / gold rate for a given currency.

**Used by:** `P2PServiceImpl.inquiry()` (LBI sell rate for p2p gold transfer)

### Request

```json
{
    "branch":    "100",
    "ccy":       "LBI",
    "historyYn": false,
    "xrateType": "CSG"
}
```

| Field | Type | Description |
|---|---|---|
| `branch` | string | Branch code — always `"100"` |
| `ccy` | string | Currency code — `"LBI"` for gold |
| `historyYn` | boolean | `false` = current rate |
| `xrateType` | string | Rate type — `"CSG"` (cash selling/buying) |

### Response

```json
{
    "code":      "CBS.MESSAGE.SUCCESS",
    "status":    0,
    "message":   "API call has been successfully completed. ",
    "journalNo": 4098296,
    "data": {
        "midRate":  1.0,
        "buyRate":  1.0,
        "sellRate": 1.0
    }
}
```

| Field | Type | Description |
|---|---|---|
| `code` | string | `"CBS.MESSAGE.SUCCESS"` on success |
| `status` | int | `0` = success |
| `data.sellRate` | decimal | Rate used to compute `total_amount` in p2p inquiry |
| `data.buyRate` | decimal | Buy rate |
| `data.midRate` | decimal | Mid rate |

### Error handling

CBS error or timeout → `RuntimeException("CBS error: ...")` → propagates as `500` to mobile app.
