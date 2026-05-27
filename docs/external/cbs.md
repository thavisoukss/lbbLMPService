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

---

## POST /api/corebanking/internalTransfer

Execute an internal fund transfer between two accounts (debit source, credit destination).

**Used by:** P2P gold transfer flow (debit sender's gold account, credit recipient's gold account)

### Request

```json
{
  "acctNo": "1000404010002961",
  "transferMode": "DR",
  "debitTranType": "TRW2",
  "creditTranType": "TRD2",
  "checkTellerLimit": "N",
  "tfrDetailList": [
    {
      "acctNo": "1000404010002961",
      "cpartyAcctNo": "1200177910002094",
      "cpartyAcctCcy": "LBI",
      "cpartyAcctStatus": "A",
      "effectDate": "2025-11-03T00:00:00+07:00",
      "cpartyAvailBal": 0.0,
      "cpartyLedgerBal": 0.0,
      "remCcy": true,
      "amount": 1,
      "equivAmount": 1,
      "drNarrative": "description",
      "crossRate": 1.0,
      "tranDate": "2025-11-03T00:00:00+07:00",
      "branch": "100"
    }
  ]
}
```

| Field | Type | Description |
|---|---|---|
| `acctNo` | string | Source (debit) account number |
| `transferMode` | string | `"DR"` = debit mode |
| `debitTranType` | string | Debit transaction type — `"TRW2"` (withdrawal gold); swap with `creditTranType` for reverse |
| `creditTranType` | string | Credit transaction type — `"TRD2"` (deposit gold); swap with `debitTranType` for reverse |
| `checkTellerLimit` | string | `"N"` = skip teller limit check |
| `tfrDetailList[].acctNo` | string | Source account number (mirrors top-level `acctNo`) |
| `tfrDetailList[].cpartyAcctNo` | string | Destination (counterparty) account number |
| `tfrDetailList[].cpartyAcctCcy` | string | Counterparty account currency — `"LBI"` for gold |
| `tfrDetailList[].cpartyAcctStatus` | string | `"A"` = active |
| `tfrDetailList[].effectDate` | string (ISO 8601) | Value date of the transfer |
| `tfrDetailList[].cpartyAvailBal` | decimal | Counterparty available balance (pass `0.0`; CBS will populate in response) |
| `tfrDetailList[].cpartyLedgerBal` | decimal | Counterparty ledger balance (pass `0.0`; CBS will populate in response) |
| `tfrDetailList[].remCcy` | boolean | `true` = remittance currency matches account currency |
| `tfrDetailList[].amount` | decimal | Transfer amount in gold units |
| `tfrDetailList[].equivAmount` | decimal | Equivalent amount (same as `amount` when `crossRate` is 1.0) |
| `tfrDetailList[].drNarrative` | string | Debit narrative / description (e.g. `"CIF {senderCif} P2P CIF {receiverCif}"`) |
| `tfrDetailList[].crossRate` | decimal | FX cross rate — `1.0` for same-currency transfers |
| `tfrDetailList[].tranDate` | string (ISO 8601) | Transaction date |
| `tfrDetailList[].branch` | string | Branch code — always `"100"` |

### Response

```json
{
  "code": "CBS.MESSAGE.SUCCESS",
  "status": 200,
  "responseType": "Success",
  "message": "API call has been successfully completed. ",
  "journalNo": 4116112,
  "details": {
    "seqNo": 51944,
    "acctNo": "1000404010002961",
    "transferMode": "DR",
    "debitTranType": "TRW2",
    "creditTranType": "TRD2",
    "checkTellerLimit": "N",
    "tfrDetailList": [
      {
        "seqNo": 51944,
        "acctNo": "1000404010002961",
        "cpartyAcctNo": "1200177910002094",
        "cpartyAcctCcy": "LBI",
        "cpartyAcctStatus": "A",
        "cpartyAvailBal": 141.5,
        "cpartyLedgerBal": 141.5,
        "effectDate": "2025-10-30T00:00:00.000+07:00",
        "remCcy": true,
        "amount": 1,
        "equivAmount": 1,
        "drNarrative": "CIF 2000987 P2P CIF 2000717",
        "crossRate": 1,
        "tranDate": "2025-10-30T00:00:00.000+07:00",
        "branch": "100",
        "drSeqNo": 51944,
        "crSeqNo": 51945,
        "availBal": 858.5,
        "ledgerBal": 858.5,
        "drFeeApplyList": [],
        "crFeeApplyList": []
      }
    ]
  },
  "warningInfoList": []
}
```

| Field | Type | Description |
|---|---|---|
| `code` | string | `"CBS.MESSAGE.SUCCESS"` on success |
| `status` | int | `200` = success |
| `journalNo` | long | CBS journal number for the transaction |
| `details.seqNo` | long | CBS sequence number for the transfer header |
| `details.tfrDetailList[].drSeqNo` | long | Debit leg sequence number |
| `details.tfrDetailList[].crSeqNo` | long | Credit leg sequence number |
| `details.tfrDetailList[].availBal` | decimal | Source account available balance after transfer |
| `details.tfrDetailList[].ledgerBal` | decimal | Source account ledger balance after transfer |
| `details.tfrDetailList[].cpartyAvailBal` | decimal | Destination account available balance after transfer |
| `details.tfrDetailList[].cpartyLedgerBal` | decimal | Destination account ledger balance after transfer |

### Error handling

Non-success `code` or HTTP error → treat as transfer failure; do not retry automatically. Log `code`, `message`, and `journalNo` if present.
