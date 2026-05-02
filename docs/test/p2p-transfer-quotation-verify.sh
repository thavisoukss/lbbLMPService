#!/bin/bash
# P2P Transfer Quotation Verify — local end-to-end test script
# Base URL: http://localhost:8084
# CR phone for testing: 2059298929

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

# ─── Step 2: P2P Inquiry ─────────────────────────────────────────────────────
echo "==> POST /api/p2p/inquiry"
INQUIRY_RESPONSE=$(curl -s -X POST http://localhost:8084/api/p2p/inquiry \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "gold_weight": 0.5,
    "cr_phone": "2059298929",
    "memo": "Test transfer"
  }')

echo "$INQUIRY_RESPONSE" | python3 -m json.tool
echo ""

REF=$(echo "$INQUIRY_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['ref'])")
Q1=$(echo "$INQUIRY_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['questions'][0]['id'])")
Q2=$(echo "$INQUIRY_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['questions'][1]['id'])")
Q3=$(echo "$INQUIRY_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['questions'][2]['id'])")
echo "ref=$REF"
echo "q1=$Q1 q2=$Q2 q3=$Q3"
echo ""

# ─── Step 3: Transfer Quotation Verify ───────────────────────────────────────
echo "==> POST /api/p2p/transfer-quotation-verify"
# Replace answer values with the real security answers for phone 2097778968
curl -s -X POST http://localhost:8084/api/p2p/transfer-quotation-verify \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"ref\": \"$REF\",
    \"first_question_id\": \"$Q1\",
    \"first_answer\": \"1234\",
    \"second_question_id\": \"$Q2\",
    \"second_answer\": \"1234\",
    \"third_question_id\": \"$Q3\",
    \"third_answer\": \"1234\"
  }" | python3 -m json.tool
