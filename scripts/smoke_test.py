#!/usr/bin/env python3
"""
End-to-end smoke test for the ecom platform.

Runs the complete customer journey (browse -> cart -> checkout -> pay -> ship ->
confirm) plus the admin side (login, dashboard, product CRUD) against a running
instance and prints a pass/fail table.

Usage:
    python3 scripts/smoke_test.py [base_url]
    base_url defaults to http://localhost:8080
"""
import json
import sys
import urllib.error
import urllib.request

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"

passed = 0
failed = 0
results = []


def call(method, path, body=None, token=None, expect=200):
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            payload = json.loads(resp.read().decode())
    except urllib.error.HTTPError as exc:
        payload = {"code": exc.code, "message": exc.read().decode()[:200], "data": None}
    if payload.get("code") != expect:
        raise AssertionError(
            f"{method} {path} -> code {payload.get('code')}: {payload.get('message')}"
        )
    # For success responses the caller wants the payload; for expected failures
    # it wants the envelope so it can assert on the message.
    return payload.get("data") if expect == 200 else payload


def check(label, fn):
    global passed, failed
    try:
        detail = fn()
        passed += 1
        results.append(("PASS", label, detail))
    except Exception as exc:  # noqa: BLE001 - report anything that breaks
        failed += 1
        results.append(("FAIL", label, str(exc)))


print(f"→ target {BASE}\n")

# ---------------------------------------------------------------- public
check("GET /api/app/home", lambda: f"{len(call('GET', '/api/app/home')['recommend'])} recommended products")
check("GET /api/app/products", lambda: f"{call('GET', '/api/app/products?limit=5')['total']} products total")

product_detail = call("GET", "/api/app/products/1")
sku = next(s for s in product_detail["product"]["skuList"] if s["stock"] > 0)
check("GET /api/app/products/1", lambda: f"{product_detail['product']['name']} · {len(product_detail['product']['skuList'])} SKUs")

# ---------------------------------------------------------------- customer
login = call("POST", "/api/app/auth/login", {"mobile": "13800138000", "password": "123456"})
token = login["token"]
check("POST /api/app/auth/login", lambda: f"member #{login['userId']} {login['nickname']}")

mine = call("GET", "/api/app/auth/mine", token=token)
check("GET /api/app/auth/mine", lambda: f"level {mine['profile']['levelName']} · cart {mine['cartCount']}")

call("DELETE", "/api/app/cart", token=token)

added = call("POST", "/api/app/cart", {"skuId": sku["id"], "quantity": 2}, token=token)
check("POST /api/app/cart", lambda: f"cart line #{added['id']} × {added['quantity']}")

cart = call("GET", "/api/app/cart", token=token)
check("GET /api/app/cart", lambda: f"{len(cart['items'])} lines · ¥{cart['checkedAmount']}")

preview = call("POST", "/api/app/orders/preview", {"source": "cart"}, token=token)
check(
    "POST /api/app/orders/preview",
    lambda: f"goods ¥{preview['totalAmount']} + freight ¥{preview['freightAmount']} - ¥{preview['discountAmount']} = ¥{preview['payAmount']}",
)

order = call("POST", "/api/app/orders", {"source": "cart", "payType": "mock"}, token=token)
check("POST /api/app/orders", lambda: f"{order['orderNo']} · ¥{order['payAmount']} · {order['statusText']}")

paid = call("POST", f"/api/app/orders/{order['orderNo']}/pay", {"payType": "mock"}, token=token)
check("POST /api/app/orders/{no}/pay", lambda: f"{paid['order']['statusText']} via {paid['order']['payType']}")

profile_after = call("GET", "/api/app/auth/profile", token=token)
check(
    "member stats after payment",
    lambda: f"spent ¥{profile_after['totalSpent']} · {profile_after['orderCount']} orders · {profile_after['points']} pts",
)

# ---------------------------------------------------------------- admin
admin = call("POST", "/api/admin/auth/login", {"username": "admin", "password": "admin123"})
admin_token = admin["token"]
check("POST /api/admin/auth/login", lambda: f"{admin['nickname']} / {admin['role']}")

overview = call("GET", "/api/admin/dashboard/overview", token=admin_token)
kpi = overview["kpi"]
check(
    "GET /api/admin/dashboard/overview",
    lambda: f"GMV30 ¥{kpi['gmv30']} · {kpi['orders30']} orders · {kpi['memberTotal']} members",
)

admin_orders = call("GET", f"/api/admin/orders?keyword={order['orderNo']}", token=admin_token)
check("GET /api/admin/orders (search)", lambda: f"{admin_orders['total']} match")

shipped = call(
    "POST",
    f"/api/admin/orders/{order['orderNo']}/ship",
    {"expressCompany": "顺丰速运", "expressNo": "SF9988776655"},
    token=admin_token,
)
check("POST /api/admin/orders/{no}/ship", lambda: f"{shipped['statusText']} · {shipped['expressNo']}")

received = call("POST", f"/api/app/orders/{order['orderNo']}/confirm", token=token)
check("POST /api/app/orders/{no}/confirm", lambda: received["statusText"])

# product create -> edit -> off shelf -> delete, all through the admin API
new_product = call(
    "POST",
    "/api/admin/products",
    {
        "name": "Smoke Test 商品",
        "categoryId": 11,
        "brandId": 1,
        "cover": "/images/products/p1.svg",
        "price": 199,
        "originalPrice": 299,
        "unit": "台",
        "status": 1,
        "specs": [{"name": "颜色", "values": ["红", "蓝"]}],
        "skus": [
            {"specValues": "红", "price": 199, "stock": 10},
            {"specValues": "蓝", "price": 219, "stock": 5},
        ],
    },
    token=admin_token,
)
check(
    "POST /api/admin/products",
    lambda: f"#{new_product['id']} {new_product['name']} · stock {new_product['stock']} · {len(new_product['skuList'])} SKUs",
)

call("PUT", f"/api/admin/products/{new_product['id']}/status?status=0", token=admin_token)
check("PUT /api/admin/products/{id}/status", lambda: "off shelf ok")

call("DELETE", f"/api/admin/products/{new_product['id']}", token=admin_token)
check("DELETE /api/admin/products/{id}", lambda: "deleted ok")

logs = call("GET", "/api/admin/logs?limit=5", token=admin_token)
check("GET /api/admin/logs", lambda: f"{logs['total']} audit rows recorded")

# ---------------------------------------------------------------- security
bad = call("GET", "/api/admin/dashboard/overview", expect=401)
check("admin API rejects anonymous", lambda: f"code {bad['code']} {bad['message']}")

cross = call("GET", "/api/admin/dashboard/overview", token=token, expect=401)
check("customer token rejected on admin API", lambda: f"code {cross['code']} {cross['message']}")

app_missing = call("GET", "/api/app/auth/mine", expect=401)
check("customer API rejects anonymous", lambda: f"code {app_missing['code']} {app_missing['message']}")

stock_guard = call("POST", "/api/app/cart", {"skuId": sku["id"], "quantity": 999999}, token=token, expect=2003)
check("oversized quantity rejected", lambda: stock_guard["message"])

# ---------------------------------------------------------------- report
print()
for status, label, detail in results:
    mark = "✓" if status == "PASS" else "✗"
    print(f"  {mark} {label:44s} {detail}")

print(f"\n{passed} passed, {failed} failed")
sys.exit(1 if failed else 0)
