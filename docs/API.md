# API overview

Two audiences, one envelope. Every response looks like:

```json
{ "code": 200, "message": "success", "data": {}, "timestamp": 1793774311000 }
```

`code !== 200` means a business failure; the HTTP status stays 200 so the
mini-program request layer keeps working. Auth failures also come back as
`code: 401` with HTTP 200.

Interactive docs: `http://localhost:8080/swagger-ui.html` (groups `1-admin`, `2-app`).

## Authentication

```
Authorization: Bearer <token>
```

The mini program may alternatively pass `?token=<token>`; the interceptor accepts
both. `X-Client-Platform: mp|web` records the origin (falls back to User-Agent
sniffing for WeChat).

| Audience | Endpoints | Issued by |
| --- | --- | --- |
| `admin` | `/api/admin/**` | `POST /api/admin/auth/login` |
| `app` | `/api/app/**` | `POST /api/app/auth/login`, `/login/mp` |

A token is only valid for its own audience.

## Admin API

### Auth

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/admin/auth/login` | `{username, password}` → token |
| GET | `/api/admin/auth/profile` | current operator |
| PUT | `/api/admin/auth/profile` | update nickname/avatar/phone/email |
| POST | `/api/admin/auth/password` | change password |

### Dashboard

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/admin/dashboard/overview` | KPIs, 14-day trend, category share, member levels, top products, top buyers |
| GET | `/api/admin/dashboard/sales-trend?days=14` | daily orders + GMV |
| GET | `/api/admin/dashboard/top-products?limit=6` | sales ranking |

### Products

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/admin/products` | paged; `keyword`, `categoryId`, `status`, `page`, `limit` |
| GET | `/api/admin/products/{id}` | detail incl. SKUs and spec matrix |
| POST | `/api/admin/products` | create (body includes `skus[]`, `specs[]`) |
| PUT | `/api/admin/products/{id}` | update |
| PUT | `/api/admin/products/{id}/status?status=1` | on/off shelf |
| DELETE | `/api/admin/products/{id}` | soft delete product + SKUs |

### Catalogue

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/admin/catalog/categories` | tree |
| GET | `/api/admin/catalog/categories/flat` | flat list (for selects) |
| POST | `/api/admin/catalog/categories` | create/update |
| DELETE | `/api/admin/catalog/categories/{id}` | delete (refuses if children/products) |
| GET/POST/DELETE | `/api/admin/catalog/brands[/{id}]` | brands |
| GET/POST/DELETE | `/api/admin/catalog/banners[/{id}]` | carousel slides |
| GET/POST/DELETE | `/api/admin/catalog/notices[/{id}]` | announcements |

### Orders

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/admin/orders` | `keyword`, `status`, `platform`, `startDate`, `endDate` |
| GET | `/api/admin/orders/{orderNo}` | detail with lines |
| POST | `/api/admin/orders/{orderNo}/ship` | `{expressCompany, expressNo}` |
| POST | `/api/admin/orders/{orderNo}/remark` | internal note |
| POST | `/api/admin/orders/{orderNo}/refund` | refund + restore stock |
| POST | `/api/admin/orders/{orderNo}/cancel` | close an unpaid order |

### Members / coupons / logs / files

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/admin/members` | `keyword`, `status`, `platform` |
| GET | `/api/admin/members/{id}` | detail |
| PUT | `/api/admin/members/{id}/status?status=1` | enable/disable |
| GET/POST | `/api/admin/coupons` | coupon templates |
| GET | `/api/admin/logs` | audit trail (`keyword`, `module`) |
| POST | `/api/admin/files/upload` | multipart `file` + `folder` |
| GET | `/api/admin/files/config` | active storage backend |

## Customer API

### Session

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | `/api/app/auth/sms?mobile=` | no | send code (demo echoes `demoCode`) |
| POST | `/api/app/auth/login` | no | `{mobile, password}` or `{mobile, smsCode}` |
| POST | `/api/app/auth/login/mp` | no | `{code}` from `wx.login()` |
| GET | `/api/app/auth/profile` | yes | current member |
| PUT | `/api/app/auth/profile` | yes | nickname / avatar / gender |
| GET | `/api/app/auth/mine` | yes | profile + order badges + cart/coupon counts |

### Catalogue (public)

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/app/home` | banners, categories, notices, recommend/new/hot |
| GET | `/api/app/products` | `keyword`, `categoryId`, `brandId`, `sort`, `minPrice`, `maxPrice` |
| GET | `/api/app/products/{id}` | product + related; increments views |
| GET | `/api/app/categories` | visible tree |
| GET | `/api/app/brands` | visible brands |
| GET | `/api/app/banners?position=home` | slides |
| GET | `/api/app/notices` | announcements |

`sort` accepts `newest` (default), `sales`, `hot`, `price_asc`, `price_desc`.

### Cart

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/app/cart` | lines + checked total |
| GET | `/api/app/cart/count` | badge count |
| POST | `/api/app/cart` | `{skuId, quantity}` — increments existing line |
| PUT | `/api/app/cart/{id}/quantity?quantity=` | change quantity (clamped to stock) |
| PUT | `/api/app/cart/{id}/checked?checked=` | toggle line |
| PUT | `/api/app/cart/checked?checked=` | select/deselect all |
| DELETE | `/api/app/cart/{id}` | remove line |
| DELETE | `/api/app/cart` | clear |

### Checkout & orders

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/app/orders/preview` | totals + address + usable coupons |
| POST | `/api/app/orders` | create order, lock stock |
| GET | `/api/app/orders?status=` | my orders |
| GET | `/api/app/orders/counts` | badge counters |
| GET | `/api/app/orders/{orderNo}` | detail |
| POST | `/api/app/orders/{orderNo}/pay` | `{payType}` |
| POST | `/api/app/orders/{orderNo}/cancel` | `{reason}` |
| POST | `/api/app/orders/{orderNo}/confirm` | confirm receipt |

`OrderCreateRequest`:

```json
{
  "source": "cart",             // cart | buy_now
  "items": [{"skuId": 1, "quantity": 2}],  // required for buy_now
  "addressId": 1,
  "couponId": 2,                // omit to let the server pick the best
  "couponOptOut": false,        // true = never auto-apply a coupon
  "remark": "请放快递柜",
  "payType": "mock"             // wechat | alipay | balance | mock
}
```

Amounts are computed server-side and never trusted from the client.

### Addresses & coupons

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/app/addresses` | address book |
| POST | `/api/app/addresses` | create (first address becomes default) |
| PUT | `/api/app/addresses/{id}` | update |
| DELETE | `/api/app/addresses/{id}` | delete and promote a new default |
| PUT | `/api/app/addresses/{id}/default` | set default |
| GET | `/api/app/coupons/mine?amount=` | wallet; `amount` annotates usability |
| GET | `/api/app/coupons/available` | claimable templates |
| POST | `/api/app/coupons/{couponId}/receive` | claim |

## Error codes

| Code | Meaning |
| --- | --- |
| 200 | success |
| 400 | bad request / validation |
| 401 | missing or invalid token |
| 403 | not the owner of the resource |
| 1002 | wrong credentials |
| 1003 | account disabled |
| 1006 | SMS code invalid or expired |
| 2001 / 2002 | product missing / off shelf |
| 2003 | insufficient stock |
| 2004 | SKU not found |
| 2005 / 2006 | category has children / products |
| 3001 | nothing checked in cart |
| 3003 | illegal order state transition |
| 3005 / 3006 | coupon unusable / already used |
| 3007 | payment failed (e.g. balance too low) |

Full list: `com.ait.common.api.ResultCode`.
