# Checkout API — Postman

Run with JDK 25 and the project's existing MySQL schema. Authentication is HTTP Basic:
username is `users.user_name`, password is the original password matching its BCrypt hash.
Role names in the database are `USER` / `ADMIN`, without the `ROLE_` prefix.

Learning-stage limitation: the user has deferred restoring the write transaction on `createOrder`.
Two existing checkout concurrency/rollback tests remain failing. The current checkout must not be
used for real orders; a success response does not guarantee that all changes were persisted.

## Routes

| Method | URL | Body | Role |
| --- | --- | --- | --- |
| GET | `/api/carts` | none | USER |
| PATCH | `/api/carts/items/{cartItemId}/quantity` | `{"quantityChange":1}` or `{"quantityChange":-1}` | USER |
| GET | `/api/discounts` | none | USER |
| POST | `/api/orders/summary` | `{}` or `{"discountId":"…"}` | USER |
| POST | `/api/orders` | `{"discountId":"…","addressId":"…","paymentId":"…"}` | USER |
| PUT | `/api/inventories/{productVariantId}/quantity` | `{"quantity":20}` | ADMIN |

`userId` is no longer accepted as the acting identity in any endpoint. The services obtain the
username from SecurityContext and look up the user's own resources. Old URLs with user/order IDs
no longer match a route. Product, discount, address and payment IDs identify selected resources.

Inventory `quantity` is the exact new nonnegative stock level. Cart `quantityChange` is a relative
change of exactly 1 or -1. The user requested this additional cart endpoint after the original five;
there are now six business routes and still no CSRF-token endpoint.

## POST, PUT and PATCH in Postman

1. Set Authorization → Basic Auth on the collection or request.
2. Call the existing `GET http://localhost:8080/api/carts` using that account.
3. Preserve the returned `XSRF-TOKEN` cookie in Postman's cookie jar.
4. Copy that cookie's value into the request header `X-XSRF-TOKEN`.
5. Send POST/PUT/PATCH with `Content-Type: application/json`, Basic Auth, the token header and cookie.

Optional Post-response script on the GET request:

```javascript
pm.collectionVariables.set("csrfToken", pm.cookies.get("XSRF-TOKEN"));
```

Then add header `X-XSRF-TOKEN: {{csrfToken}}` to write requests. Fetch a fresh token after changing
accounts or clearing cookies. GET requests need Basic Auth but no CSRF token. An ADMIN-only account
receives 403 for the cart GET but still receives the CSRF cookie; it can then update inventory.
Alternatively, a first POST/PUT/PATCH without a token returns 403 and the cookie; copy its value into
the header and repeat the request. A cookie without the matching header is not enough.

This uses Spring Security's built-in `csrf.spa()` configuration, with no custom token endpoint
or filter. See [Spring Security CSRF documentation](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa).

For authorization tests: missing/wrong credentials on GET produce 401; authenticated USER writing
inventory with a valid CSRF token produces 403; a write without CSRF token also produces 403.
A foreign, used, revoked or deleted discount produces 400. Discount dates are intentionally
not checked yet: expired or future-dated assignments are allowed if the other conditions pass.

## Cart plus/minus buttons

Read `data.items[].cartItemId` from `GET /api/carts`, then call
`PATCH /api/carts/items/{cartItemId}/quantity` with `{"quantityChange":1}` for `+` or
`{"quantityChange":-1}` for `-`. The response's `data` is the new quantity.

Only the signed-in user's active cart item can be changed; another user's, missing or deleted
item returns 404. Missing/null/zero or a change outside -1 and 1 returns 400. At zero the item is
soft-deleted, not physically removed; the endpoint cannot re-add a deleted item. Increasing past
available stock returns 400, including when the same variant occurs on multiple cart lines.
Decreasing remains possible after stock drops. Changing the cart does not reserve or reduce stock.
Concurrent clicks and checkout use the same cart lock. Each successful PATCH applies one step,
so do not automatically retry it after an ambiguous network timeout; reload the cart first.

Call the existing cart or summary API after a change to refresh displayed totals.

## Calculation and verification

Shipping is fixed at 30000.00 per order for both preview and creation, with no additional pricing
rules or configuration. `total = subtotal - discountAmount + shippingFee`; discounts apply only
to the product subtotal, not the shipping fee.

For subtotal 25.00 with 10% discount, summary returns discountAmount 2.50, shippingFee 30000.00,
total 30022.50. Creation recalculates using the current cart and prices; a preview does not reserve
stock or freeze prices. Orders begin at PENDING. Repeating checkout against the cleared cart fails.

To change the discount after preview, pass the newly selected `discountId` to the existing
`POST /api/orders` request:

```json
{
  "discountId": "new-selected-discount-id",
  "addressId": "address-id",
  "paymentId": "payment-id"
}
```

The server checks that this discount belongs to the current user and is still eligible, then
recalculates and consumes only that discount. Send `null` or omit `discountId` to use no discount,
even if the preview had one. Do not send `value`, `discountAmount` or `total`; client-supplied
amounts are not used. To display the updated total before confirmation, call the existing
summary endpoint again with the newly selected `discountId`.

Run all tests with `JAVA_HOME` pointing to JDK 25: `./mvnw clean test`.

Before using real data, confirm the discount statuses (`AVAILABLE` / `USED`) and boolean `used_at`
mapping match your DB. Address/payment ownership still requires their schema; this change does
not claim those IDs belong to the authenticated user. See `.scratch/secure-checkout/spec.md`.
