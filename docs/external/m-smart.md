## Response Convention

All m-smart API responses use `responseCode` to indicate the result:

| `responseCode` | Meaning |
|----------------|---------|
| `"0000"` | Success |
| anything else | Error |

---

## m-smart API paths (configured under `external.api.m-smart`)
| Key | Path |
|-----|------|
| member-list | `/m-smart/lmps/member-list` |
| inquiry out | `/m-smart/lmps/out/inquiry/register` |
| transfer out | `/m-smart/lmps/out/transfer` |
| build QR | `/m-smart/lmps/qr/generate` |
| QR info | `/m-smart/lmps/qr/info` |

### POST - QR info - `/m-smart/lmps/qr/info`

**Request body**
```json
{
  "clientInfo": {
    "deviceId": "device1234567",
    "mobileNo": "2059355555",
    "userId": "user001"
  },
  "securityContext": {
    "channel": "MOBILE"
  },
  "data": {
    "qrString": "00020101021238680016A00526628466257701087003041802030010325LRV25288QOSY68AFCZJ7PUE1V520448295303418540815000.005802LA6009Vientiane62160812test_dynamic63040B1B" // LDB Dynamic
  }
}
```

**Response `200 OK`**
```json
{
  "responseCode": "0000",
  "responseMessage": "Transaction completed successfully",
  "responseStatus": "SUCCESS",
  "responseTimestamp": "2025-10-20T08:32:28.125243700Z",
  "clientInfo": {
    "deviceId": "device1234567",
    "mobileNo": "2059355555",
    "userId": "user001"
  },
  "data": {
    "payloadFormatIndicator": "01",
    "pointOfInitiation": "12",
    "applicationId": "A005266284662577",
    "iin": "70030418",
    "qrPaymentType": "001",
    "receiverId": "LRV25288QOSY68AFCZJ7PUE1V",
    "mcc": "4829",
    "txnCurrency": "418",
    "txnAmount": "15000.00",
    "countryCode": "LA",
    "name": null,
    "city": "Vientiane",
    "purposeOfTxn": "test_dynamic",
    "memberId": "LDB",
    "memberName": "LDB"
  }
}
```


### POST - inquiry out - `/m-smart/lmps/out/inquiry/register`
`"toType": "ACCOUNT"` for account and `"toType": "QR"` for QR

**Request body**
```json
{
  "clientInfo": {
    "deviceId": "iPhone14-ABCD1234EFGH5678",
    "mobileNo": "2055555999",
    "userId": "user001"
  },
  "securityContext": {
    "channel": "MSMART"
  },
  "data": {
    "txnId": "9900007",
    "fromuser": "user123",
    "fromaccount": "1200174910002059",
    "fromCif": "2001749",   // client No
    "toType": "ACCOUNT",              // "ACCOUNT" or "QR"
    "toaccount": "0100000947725",     // ACCOUNT: destination account no. | QR: raw QR string value
    "tomember": "IB"
  }
}
```

**Response `200 OK`**
```json
{
  "responseCode": "0000",
  "responseMessage": "Transaction completed successfully",
  "responseStatus": "SUCCESS",
  "responseTimestamp": "2026-04-03T06:24:03.678460993Z",
  "clientInfo": {
    "deviceId": "iPhone14-ABCD1234EFGH5678",
    "mobileNo": "2055555999",
    "userId": "user001"
  },
  "data": {
    "txnAmount": null,
    "txnId": "9900007",
    "frommember": "LBB",
    "fromuser": "user123",
    "fromaccount": "1200174910002059",
    "fromCif": null,
    "toType": "ACCOUNT",
    "toaccount": "0100000947725",
    "tomember": "IB",
    "reference": "LBB7TWU9VZSGASW4HKG7466",
    "accountname": "MR AXXXXXXXXX XXXXXXXXXX",
    "accountccy": "LAK",
    "feelist": {
      "LAK": [
        {
          "from": 0.00,
          "feeamount": 1000.00
        },
        {
          "from": 2000001.00,
          "feeamount": 1500.00
        },
        {
          "from": 3000001.00,
          "feeamount": 2500.00
        },
        {
          "from": 4000001.00,
          "feeamount": 3000.00
        },
        {
          "from": 5000001.00,
          "feeamount": 4500.00
        },
        {
          "from": 7000001.00,
          "feeamount": 7500.00
        },
        {
          "from": 10000001.00,
          "feeamount": 12000.00
        },
        {
          "from": 30000001.00,
          "feeamount": 15500.00
        },
        {
          "from": 50000001.00,
          "feeamount": 20000.00
        },
        {
          "from": 100000001.00,
          "feeamount": 25000.00
        },
        {
          "from": 120000001.00,
          "feeamount": 30000.00
        },
        {
          "from": 150000001.00,
          "feeamount": 40000.00
        },
        {
          "from": 200000001.00,
          "feeamount": 50000.00
        }
      ],
      "USD": null,
      "THB": null
    }
  }
}
```

