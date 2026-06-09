#!/bin/bash
# Production Member List — test script
# Base URL: http://localhost:8084

# ─── Step 1: Login to Production ──────────────────────────────────────────────
echo "==> Login to Production"
LOGIN_RESPONSE=$(curl -s -k -X POST https://prodapi.laobullionbank.com/lbb-customer-api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "phone": "2059366665",
    "password": "ZAQwsx123!@#"
  }')

TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys,json;
try:
    print(json.load(sys.stdin)['data']['access_token'])
except Exception as e:
    print('ERROR: Failed to extract token from login response', file=sys.stderr)
    print(e, file=sys.stderr)
    sys.exit(1)
")

if [ $? -ne 0 ] || [ -z "$TOKEN" ]; then
  echo "Login failed. Response details:"
  echo "$LOGIN_RESPONSE"
  exit 1
fi

echo "Login successful. Extracted token prefix: ${TOKEN:0:20}..."
echo ""

# ─── Step 2: Call Local Endpoint with Prod JWT ────────────────────────────────
echo "==> GET http://localhost:8084/payment/lmps/get-member-list"
curl -s -X GET http://localhost:8084/payment/lmps/get-member-list \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -H "Device-ID: test-device-123" | python3 -m json.tool
