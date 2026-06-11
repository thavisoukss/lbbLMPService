#!/bin/bash
# API Notification — local/UAT test script
# Usage: ./api-notification.sh [phone] [title] [desc] [env]
# Example: ./api-notification.sh "2059298929" "Withdraw" "Withdraw successful" "dev"

PHONE=${1:-"2059298929"}
TITLE=${2:-"Withdraw"}
DESC=${3:-"Transaction successful"}
ENV=${4:-"dev"}

if [ "$ENV" = "prod" ]; then
  URL="http://172.16.0.40:2565/lbb-customer-api/v1/lmpt-callback"
  API_KEY="A3STRPQFKB2C4JX589"
else
  URL="http://172.16.4.40:2565/lbb-customer-api/v1/lmpt-callback"
  API_KEY="A3STRPQFKB2C4JX588"
fi

# Generate UUID for X-Nonce
NONCE=$(uuidgen | tr '[:upper:]' '[:lower:]')

echo "==> Sending API Notification to target environment: $ENV"
echo "URL: $URL"
echo "API-KEY: $API_KEY"
echo "X-Nonce: $NONCE"
# Define JSON Body
BODY=$(cat <<EOF
{
  "title": "$TITLE",
  "desc": "$DESC",
  "phone": "$PHONE"
}
EOF
)

echo "Body:"
echo "$BODY"
echo "--------------------------------------------------"

echo "Copy-pasteable curl command:"
echo "curl --connect-timeout 5 -i -X POST \"$URL\" \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -H \"API-KEY: $API_KEY\" \\"
echo "  -H \"X-Nonce: $NONCE\" \\"
echo "  -d '$BODY'"
echo "--------------------------------------------------"

curl --connect-timeout 5 -i -X POST "$URL" \
  -H "Content-Type: application/json" \
  -H "API-KEY: $API_KEY" \
  -H "X-Nonce: $NONCE" \
  -d "$BODY"
echo ""
