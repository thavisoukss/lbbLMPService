#!/usr/bin/env python3
import os
import sys
import time
import secrets
import subprocess
import base64
import argparse
import json

# Default configuration
DEFAULT_PRIVATE_KEY = "src/main/resources/keys/uat-private.pem"
DEFAULT_PUBLIC_KEY = "src/main/resources/keys/uat-public.pem"

def main():
    parser = argparse.ArgumentParser(description="Generate Biometric signature and secret for LBB Plus Transaction.")
    parser.add_argument("--phone", help="User phone number (default: 2097778968 for UAT, 2059366665 for Prod)")
    parser.add_argument("--secret", help="Biometric secret (default: auto-generated)")
    parser.add_argument("--nonce", default="x_nonce_placeholder", help="Transaction x_nonce (default: x_nonce_placeholder)")
    parser.add_argument("--amount", default="100000", help="Transaction amount (default: 100000)")
    parser.add_argument("--qr", default="00020101021226580011hk.com.bualuang.qr0108101416450208123456785204531153037645802LA", help="QR string for the transaction")
    parser.add_argument("--customer-id", default="CUST001", help="Customer ID for SQL statement generation")
    parser.add_argument("--purpose", default="Biometric Test Transfer", help="Purpose of transaction")
    parser.add_argument("--private-key", help="Path to private key PEM file")
    parser.add_argument("--public-key", help="Path to public key PEM file")
    parser.add_argument("--timestamp", help="Biometric timestamp (default: current epoch seconds)")
    parser.add_argument("--env", default="uat", choices=["uat", "prod"], help="Environment: uat or prod (default: uat)")
    args = parser.parse_args()

    # Determine default configuration based on environment
    if args.env == "prod":
        default_private = "/Users/nohder/Library/CloudStorage/GoogleDrive-noh.sayachack@gmail.com/My Drive/obsidian/NohDer/01-Projects/_personal/lbbplus/pro-private.pem"
        default_public = "src/main/resources/keys/prod-public.pem"
        default_phone = "2059366665"
    else:
        default_private = "src/main/resources/keys/uat-private.pem"
        default_public = "src/main/resources/keys/uat-public.pem"
        default_phone = "2097778968"

    private_key_file = args.private_key if args.private_key else default_private
    public_key_file = args.public_key if args.public_key else default_public

    # 1. Generate/verify keys
    if not os.path.exists(private_key_file):
        print(f"[-] Private key file not found: {private_key_file}", file=sys.stderr)
        print(f"[*] Generating temporary key pair local to execution directory...")
        private_key_file = "private_key.pem"
        public_key_file = "public_key.pem"
        if not os.path.exists(private_key_file) or not os.path.exists(public_key_file):
            try:
                subprocess.run(["openssl", "genrsa", "-out", private_key_file, "2048"], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                subprocess.run(["openssl", "rsa", "-pubout", "-in", private_key_file, "-out", public_key_file], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                print("[+] Temporary key pair generated successfully in execution directory.")
            except Exception as e:
                print(f"[-] Failed to generate keys: {e}", file=sys.stderr)
                sys.exit(1)

    # If public key doesn't exist but private key does, derive it
    if not os.path.exists(public_key_file) and os.path.exists(private_key_file):
        try:
            subprocess.run(["openssl", "rsa", "-pubout", "-in", private_key_file, "-out", public_key_file], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print(f"[+] Derived public key from {private_key_file} to {public_key_file}")
        except Exception as e:
            print(f"[-] Failed to derive public key: {e}", file=sys.stderr)
            sys.exit(1)

    print(f"[*] Using keys:")
    print(f"    - Private Key: {private_key_file}")
    print(f"    - Public Key: {public_key_file}")

    # 2. Read public key
    with open(public_key_file, "r") as f:
        public_key_pem = f.read().strip()

    # 3. Prepare variables
    if args.timestamp:
        timestamp = args.timestamp
    else:
        from datetime import datetime, timezone
        timestamp = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + 'Z'
    secret = args.secret if args.secret else secrets.token_hex(16)
    phone = args.phone if args.phone else default_phone
    
    # 4. Construct payload
    payload = f"{timestamp}|{phone}|{secret}"
    print(f"\n[*] Constructed Payload: {payload}")

    # 5. Sign the payload using openssl dgst -sha256 -sign private_key_file
    try:
        process = subprocess.Popen(
            ["openssl", "dgst", "-sha256", "-sign", private_key_file],
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        signature_bytes, stderr = process.communicate(input=payload.encode('utf-8'))
        
        if process.returncode != 0:
            raise Exception(stderr.decode('utf-8'))
            
        signature_b64 = base64.b64encode(signature_bytes).decode('utf-8')
    except Exception as e:
        print(f"[-] Failed to sign payload: {e}", file=sys.stderr)
        sys.exit(1)

    # 6. Verify signature locally to ensure correctness
    try:
        temp_payload_file = "temp_payload.txt"
        temp_signature_file = "temp_signature.bin"
        
        with open(temp_payload_file, "w") as f:
            f.write(payload)
            
        with open(temp_signature_file, "wb") as f:
            f.write(signature_bytes)
            
        verify_process = subprocess.run(
            ["openssl", "dgst", "-sha256", "-verify", public_key_file, "-signature", temp_signature_file, temp_payload_file],
            capture_output=True,
            text=True
        )
        
        # Cleanup
        os.remove(temp_payload_file)
        os.remove(temp_signature_file)
        
        if verify_process.returncode == 0 and "Verified OK" in verify_process.stdout:
            print("[+] Cryptographic verification passed: Signature is VALID!")
        else:
            print(f"[-] Cryptographic verification failed! {verify_process.stderr}", file=sys.stderr)
    except Exception as e:
        print(f"[!] Warning: Could not run verification checks: {e}")

    # 7. Print results
    print("\n" + "="*80)
    print("PUBLIC KEY (PEM format to save in CUSTOMER.BIO_KEY)")
    print("="*80)
    print(public_key_pem)
    
    print("\n" + "="*80)
    print("SQL UPDATE STATEMENT (Update BIO_KEY in Database)")
    print("="*80)
    sql_pem = public_key_pem.replace("'", "''")
    print(f"UPDATE customer SET bio_key = '{sql_pem}' WHERE id = '{args.customer_id}';")
    print("COMMIT;")

    print("\n" + "="*80)
    print("REQUEST PAYLOAD FOR POST /payment/lmps/transfer-out-qr-bio-verify")
    print("="*80)
    req_body = {
        "x_nonce": args.nonce,
        "qr_string": args.qr,
        "amount": float(args.amount),
        "purpose": args.purpose,
        "timestamp": timestamp,
        "secret": secret,
        "signature": signature_b64
    }
    print(json.dumps(req_body, indent=2))
    
    print("\n" + "="*80)
    print("CURL COMMAND EXAMPLE")
    print("="*80)
    print(f"curl -X POST http://localhost:8084/payment/lmps/transfer-out-qr-bio-verify \\")
    print(f"  -H 'Content-Type: application/json' \\")
    print(f"  -H 'Device-ID: test-device-123' \\")
    print(f"  -H 'Authorization: Bearer <YOUR_ACCESS_TOKEN>' \\")
    print(f"  -d '{json.dumps(req_body)}'")
    print("="*80)

if __name__ == "__main__":
    main()
