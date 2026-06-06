# Biometric Signature Generation Guide

This guide describes how to generate biometric secrets and cryptographic signatures for transaction verification and biometric login within the LBB Plus platform. It covers instructions for both the **UAT** and **Production** environments using the helper Python script.

---

## Prerequisites
1. **Python 3.6+**
2. **OpenSSL** (pre-installed on macOS)
3. The helper script located at [docs/test/generate-bio-signature.py](file:///Users/nohder/dev/lbb/lbbLMPService/docs/test/generate-bio-signature.py)

---

## 1. Key Configuration

| Environment | Private Key Location | Public Key Location | Target Phone Number |
| :--- | :--- | :--- | :--- |
| **UAT** | `src/main/resources/keys/uat-private.pem` | `src/main/resources/keys/uat-public.pem` | `2097778968` |
| **Production** | `/Users/nohder/Library/CloudStorage/GoogleDrive-noh.sayachack@gmail.com/My Drive/obsidian/NohDer/01-Projects/_personal/lbbplus/pro-private.pem` | `src/main/resources/keys/prod-public.pem` | `2059366665` |

---

## 1.5 Generating a New Key Pair

To generate a new RSA key pair for biometric verification:

1. **Generate the private key (2048-bit RSA):**
   ```bash
   openssl genrsa -out private_key.pem 2048
   ```

2. **Derive the public key in PKIX/X.509 format (PEM):**
   ```bash
   openssl rsa -pubout -in private_key.pem -out public_key.pem
   ```

The contents of `public_key.pem` can then be stored in the database's `CUSTOMER.BIO_KEY` column.

---

## 2. Generation Commands

The script supports the `--env` flag, which automatically configures the correct private key, public key, and default phone number for the environment.

### A. Generating for UAT (Mobile: 2097778968)
Run the script without arguments or with `--env uat` (default):

```bash
python3 docs/test/generate-bio-signature.py --env uat
```

*For a custom transaction amount/nonce:*
```bash
python3 docs/test/generate-bio-signature.py \
  --env uat \
  --nonce "<YOUR_X_NONCE>" \
  --amount 100000
```

---

### B. Generating for Production (Mobile: 2059366665)
Run the script using the `--env prod` flag:

```bash
python3 docs/test/generate-bio-signature.py --env prod
```

*For a custom transaction amount/nonce:*
```bash
python3 docs/test/generate-bio-signature.py \
  --env prod \
  --nonce "<YOUR_X_NONCE>" \
  --amount 100000
```

---

## 3. Script CLI Arguments Reference

| Argument | Description | Default |
| :--- | :--- | :--- |
| `--env` | Environment configuration (`uat` or `prod`) | `uat` |
| `--phone` | Overrides the default phone number | `2097778968` (UAT) / `2059366665` (Prod) |
| `--timestamp` | The timestamp format used in the request body | Current UTC time in ISO 8601 format (`YYYY-MM-DDTHH:MM:SS.sssZ`) |
| `--secret` | Custom biometric secret to sign | Auto-generated 32-character hex string |
| `--nonce` | The transaction `x_nonce` identifier | `x_nonce_placeholder` |
| `--amount` | The transaction amount | `100000` |
| `--qr` | QR string parameter | Placeholder QR string |
| `--private-key` | Path to the private key used for signing | Automatically set by `--env` |
| `--public-key` | Path to the public key to compare/save | Automatically set by `--env` |

---

## 4. Payload Structure & Signature Verification
The verification payload is formed by joining the parameters with a pipe (`|`):
```text
Payload = Timestamp|UserPhone|Secret
```

**Example:**
`2026-06-06T08:39:40.361Z|2059366665|8ffd7ba9886b0b609c1c9139cd5e2bc1`

The signature is computed over the SHA-256 hash of this payload, encrypted with RSA (PKCS#1 v1.5 padding), and then Base64-encoded.
