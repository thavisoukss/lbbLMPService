#!/bin/bash
# P2P Inquiry — local test script
# Base URL: http://localhost:8084

# ─── Step 1: Login ───────────────────────────────────────────────────────────
echo "==> Login"
LOGIN_RESPONSE=$(curl -s -X POST https://api2-uat.laobullionbank.com/lbb-customer-api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "phone": "2097778968",
    "password": "P@r97778968"
  }')

TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['access_token'])")
echo "Token: $TOKEN"
echo ""

# ─── Step 2: Get Account Info by Phone ───────────────────────────────────────
echo "==> POST /api/p2p/get-account-info-by-phone"
curl -s -X POST http://localhost:8084/api/p2p/get-account-info-by-phone \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "cr_phone": "2059298929"
  }' | python3 -m json.tool
echo ""

# ─── Step 3: P2P Inquiry ─────────────────────────────────────────────────────
echo "==> POST /api/p2p/inquiry"
curl -s -X POST http://localhost:8084/api/p2p/inquiry \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "gold_weight": 0.5,
    "cr_phone": "2059298929",
    "memo": "Transfer memo"
  }' | python3 -m json.tool
