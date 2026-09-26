#!/usr/bin/env bash
set -euo pipefail

BASE="http://localhost:8080"
WORKDIR="$(mktemp -d)"
LAST_BODY=""
LAST_STATUS=""

request() {
  local label="$1"
  local method="$2"
  local url="$3"
  local data="${4-}"
  local auth="${5-}"
  local outfile="$WORKDIR/response.json"
  local statusfile="$WORKDIR/status.txt"
  rm -f "$outfile" "$statusfile"

  echo "===== $label ====="
  echo "$method $url"

  if [[ -n "$auth" && -n "$data" ]]; then
    curl -sS -X "$method" "$url" \
      -H 'Content-Type: application/json' \
      -H "Authorization: Bearer $auth" \
      -d "$data" \
      -o "$outfile" -w '%{http_code}' > "$statusfile"
  elif [[ -n "$auth" ]]; then
    curl -sS -X "$method" "$url" \
      -H "Authorization: Bearer $auth" \
      -o "$outfile" -w '%{http_code}' > "$statusfile"
  elif [[ -n "$data" ]]; then
    curl -sS -X "$method" "$url" \
      -H 'Content-Type: application/json' \
      -d "$data" \
      -o "$outfile" -w '%{http_code}' > "$statusfile"
  else
    curl -sS -X "$method" "$url" \
      -o "$outfile" -w '%{http_code}' > "$statusfile"
  fi

  LAST_STATUS="$(cat "$statusfile")"
  LAST_BODY="$(cat "$outfile")"
  echo "HTTP $LAST_STATUS"
  cat "$outfile"
  echo
  echo
}

json_get() {
  local expr="$1"
  printf '%s' "$LAST_BODY" | python3 -c 'import json,sys
obj = json.load(sys.stdin)
expr = sys.argv[1]
parts = expr.split(".") if expr else []
cur = obj
for part in parts:
    cur = cur[int(part)] if part.isdigit() else cur[part]
if isinstance(cur, (dict, list)):
    print(json.dumps(cur))
elif cur is None:
    print("")
else:
    print(cur)' "$expr"
}

assert_status() {
  local expected="$1"
  if [[ "$LAST_STATUS" != "$expected" ]]; then
    echo "Expected HTTP $expected but got $LAST_STATUS"
    exit 1
  fi
}

request 'Auth greeting via gateway' GET "$BASE/auth/greeting"
assert_status 200

request 'Product greeting via gateway' GET "$BASE/products/greeting"
assert_status 200

request 'Register admin' POST "$BASE/auth/register" '{"email":"admin@example.com","password":"Password123","roles":["ADMIN","USER"]}'
assert_status 201
ADMIN_USER_ID="$(json_get id)"

request 'Register shopkeeper' POST "$BASE/auth/register" '{"email":"shopkeeper@example.com","password":"Password123","roles":["SHOPKEEPER","USER"]}'
assert_status 201
SHOPKEEPER_USER_ID="$(json_get id)"

request 'Register buyer' POST "$BASE/auth/register" '{"email":"buyer@example.com","password":"Password123","roles":["USER"]}'
assert_status 201
BUYER_USER_ID="$(json_get id)"

request 'Admin login #1' POST "$BASE/auth/login" '{"email":"admin@example.com","password":"Password123","role":"ADMIN"}'
assert_status 200
ADMIN_TOKEN_1="$(json_get accessToken)"
ADMIN_SESSION_1="$(json_get sessionId)"

echo "Captured ADMIN_USER_ID=$ADMIN_USER_ID"
echo "Captured SHOPKEEPER_USER_ID=$SHOPKEEPER_USER_ID"
echo "Captured BUYER_USER_ID=$BUYER_USER_ID"
echo "Captured ADMIN_SESSION_1=$ADMIN_SESSION_1"
echo

request 'Admin me' GET "$BASE/auth/me" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Admin lookup buyer by email' GET "$BASE/auth/users/by-email?email=buyer@example.com" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Create seller profile' POST "$BASE/products/admin/sellers" "{\"userId\":\"$SHOPKEEPER_USER_ID\",\"description\":\"Trusted electronics seller\",\"address\":\"Pune, India\",\"latitude\":18.520430,\"longitude\":73.856743,\"creditScore\":780}" "$ADMIN_TOKEN_1"
assert_status 201

request 'Get seller profile' GET "$BASE/products/admin/sellers/$SHOPKEEPER_USER_ID" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Update seller profile' PUT "$BASE/products/admin/sellers/$SHOPKEEPER_USER_ID" "{\"userId\":\"$SHOPKEEPER_USER_ID\",\"description\":\"Top rated seller\",\"address\":\"Baner, Pune\",\"latitude\":18.559000,\"longitude\":73.786800,\"creditScore\":820}" "$ADMIN_TOKEN_1"
assert_status 200

request 'List sellers' GET "$BASE/products/admin/sellers" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Create category' POST "$BASE/products/admin/categories" '{"name":"Mobiles","description":"Smartphones and accessories","active":true}' "$ADMIN_TOKEN_1"
assert_status 201
CATEGORY_ID="$(json_get id)"

request 'Update category' PUT "$BASE/products/admin/categories/$CATEGORY_ID" '{"name":"Mobiles & Tablets","description":"Smartphones, tablets and accessories","active":true}' "$ADMIN_TOKEN_1"
assert_status 200

request 'List admin categories' GET "$BASE/products/admin/categories" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Create product' POST "$BASE/products/admin" "{\"name\":\"Galaxy Ultra X\",\"categoryId\":\"$CATEGORY_ID\",\"price\":89999.00,\"oldPrice\":99999.00,\"rating\":4.50,\"description\":\"Flagship Android device\",\"sellerUserId\":\"$SHOPKEEPER_USER_ID\",\"active\":true}" "$ADMIN_TOKEN_1"
assert_status 201
PRODUCT_ID="$(json_get id)"

request 'Update product' PUT "$BASE/products/admin/$PRODUCT_ID" "{\"name\":\"Galaxy Ultra X Pro\",\"categoryId\":\"$CATEGORY_ID\",\"price\":91999.00,\"oldPrice\":99999.00,\"rating\":4.60,\"description\":\"Upgraded flagship Android device\",\"sellerUserId\":\"$SHOPKEEPER_USER_ID\",\"active\":true}" "$ADMIN_TOKEN_1"
assert_status 200

request 'Get admin product details' GET "$BASE/products/admin/$PRODUCT_ID" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'List admin products' GET "$BASE/products/admin" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Create variant' POST "$BASE/products/admin/$PRODUCT_ID/variants" '{"name":"12GB / 256GB","sku":"GALAXY-ULTRA-X-PRO-256","price":91999.00,"oldPrice":99999.00,"stockQuantity":25,"attributes":[{"label":"Color","value":"Obsidian"},{"label":"Storage","value":"256GB"},{"label":"RAM","value":"12GB"}],"active":true}' "$ADMIN_TOKEN_1"
assert_status 201
VARIANT_ID="$(json_get id)"

request 'Update variant' PUT "$BASE/products/admin/$PRODUCT_ID/variants/$VARIANT_ID" '{"name":"12GB / 512GB","sku":"GALAXY-ULTRA-X-PRO-512","price":94999.00,"oldPrice":102999.00,"stockQuantity":18,"attributes":[{"label":"Color","value":"Titan Black"},{"label":"Storage","value":"512GB"},{"label":"RAM","value":"12GB"}],"active":true}' "$ADMIN_TOKEN_1"
assert_status 200

request 'List variants' GET "$BASE/products/admin/$PRODUCT_ID/variants" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Create product image' POST "$BASE/products/admin/$PRODUCT_ID/images" '{"imageUrl":"https://cdn.example.com/products/galaxy-ultra-x-pro/front.jpg","altText":"Front view of Galaxy Ultra X Pro","primary":true,"displayOrder":0}' "$ADMIN_TOKEN_1"
assert_status 201
IMAGE_ID="$(json_get id)"

request 'Update product image' PUT "$BASE/products/admin/$PRODUCT_ID/images/$IMAGE_ID" '{"imageUrl":"https://cdn.example.com/products/galaxy-ultra-x-pro/front-hero.jpg","altText":"Hero front view of Galaxy Ultra X Pro","primary":true,"displayOrder":0}' "$ADMIN_TOKEN_1"
assert_status 200

request 'List product images' GET "$BASE/products/admin/$PRODUCT_ID/images" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Create specification' POST "$BASE/products/admin/$PRODUCT_ID/specifications" '{"label":"Display","value":"6.8","unit":"inch","displayOrder":0}' "$ADMIN_TOKEN_1"
assert_status 201
SPEC_ID="$(json_get id)"

request 'Update specification' PUT "$BASE/products/admin/$PRODUCT_ID/specifications/$SPEC_ID" '{"label":"Battery","value":"5000","unit":"mAh","displayOrder":1}' "$ADMIN_TOKEN_1"
assert_status 200

request 'List specifications' GET "$BASE/products/admin/$PRODUCT_ID/specifications" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Public categories' GET "$BASE/products/categories"
assert_status 200

request 'Public products by seller filter' GET "$BASE/products?sellerUserId=$SHOPKEEPER_USER_ID"
assert_status 200

request 'Public product details' GET "$BASE/products/$PRODUCT_ID"
assert_status 200

request 'Public reviews before add' GET "$BASE/products/$PRODUCT_ID/reviews"
assert_status 200

request 'Buyer login' POST "$BASE/auth/login" '{"email":"buyer@example.com","password":"Password123","role":"USER"}'
assert_status 200
BUYER_TOKEN="$(json_get accessToken)"

request 'Buyer add review' POST "$BASE/products/$PRODUCT_ID/reviews" '{"rating":5,"title":"Excellent phone","comment":"Battery and camera are both great."}' "$BUYER_TOKEN"
assert_status 201
REVIEW_ID="$(json_get id)"

request 'Public reviews after add' GET "$BASE/products/$PRODUCT_ID/reviews"
assert_status 200

request 'Admin list reviews' GET "$BASE/products/admin/$PRODUCT_ID/reviews" '' "$ADMIN_TOKEN_1"
assert_status 200

request 'Admin login #2 same email+role' POST "$BASE/auth/login" '{"email":"admin@example.com","password":"Password123","role":"ADMIN"}'
assert_status 200
ADMIN_TOKEN_2="$(json_get accessToken)"
ADMIN_SESSION_2="$(json_get sessionId)"
echo "Captured ADMIN_SESSION_2=$ADMIN_SESSION_2"
echo

request 'Old admin token should now be invalid' GET "$BASE/auth/me" '' "$ADMIN_TOKEN_1"
assert_status 401

request 'New admin token should work' GET "$BASE/auth/me" '' "$ADMIN_TOKEN_2"
assert_status 200

request 'Delete review' DELETE "$BASE/products/admin/reviews/$REVIEW_ID" '' "$ADMIN_TOKEN_2"
assert_status 204

request 'Delete image' DELETE "$BASE/products/admin/$PRODUCT_ID/images/$IMAGE_ID" '' "$ADMIN_TOKEN_2"
assert_status 204

request 'Delete variant' DELETE "$BASE/products/admin/$PRODUCT_ID/variants/$VARIANT_ID" '' "$ADMIN_TOKEN_2"
assert_status 204

request 'Delete product' DELETE "$BASE/products/admin/$PRODUCT_ID" '' "$ADMIN_TOKEN_2"
assert_status 204

request 'Delete category' DELETE "$BASE/products/admin/categories/$CATEGORY_ID" '' "$ADMIN_TOKEN_2"
assert_status 204

request 'Delete seller' DELETE "$BASE/products/admin/sellers/$SHOPKEEPER_USER_ID" '' "$ADMIN_TOKEN_2"
assert_status 204

request 'Logout admin token #2' POST "$BASE/auth/logout" '' "$ADMIN_TOKEN_2"
assert_status 200

request 'Logout buyer token' POST "$BASE/auth/logout" '' "$BUYER_TOKEN"
assert_status 200

echo '===== SEQUENCE COMPLETE ====='
echo "ADMIN_USER_ID=$ADMIN_USER_ID"
echo "SHOPKEEPER_USER_ID=$SHOPKEEPER_USER_ID"
echo "BUYER_USER_ID=$BUYER_USER_ID"
echo "CATEGORY_ID=$CATEGORY_ID"
echo "PRODUCT_ID=$PRODUCT_ID"
echo "VARIANT_ID=$VARIANT_ID"
echo "IMAGE_ID=$IMAGE_ID"
echo "SPEC_ID=$SPEC_ID"
echo "REVIEW_ID=$REVIEW_ID"

