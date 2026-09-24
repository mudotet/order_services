# Checkout API - Postman

Run with JDK 25 and the project's existing MySQL schema. Authentication is HTTP Basic:
the Username field contains `users.email`, and the password is the original password matching its BCrypt hash.
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
| GET | `/api/orders/tracking/{id}` | none | USER, order owner |
| PUT | `/api/inventories/{productVariantId}/quantity` | `{"quantity":20}` | ADMIN |

`userId` is no longer accepted as the acting identity in any endpoint.
The services obtain the authenticated email from SecurityContext, resolve the account, and use its database ID to look up the user's own resources.
The tracking URL accepts an order ID as the selected resource, and the repository checks its owner.
Product, discount, address and payment IDs identify other selected resources.

Inventory `quantity` is the exact new nonnegative stock level. Cart `quantityChange` is a relative
change of exactly 1 or -1.
The table covers checkout, inventory, and order tracking.

## POST, PUT and PATCH in Postman

1. Set Authorization → Basic Auth on the collection or request.
2. Send POST/PUT/PATCH with `Content-Type: application/json`, Basic Auth and the request body.

Alternatively, log in through `POST /api/auth/login` and keep the returned `JSESSIONID` cookie instead of using Basic Auth.
No token bootstrap request or `X-XSRF-TOKEN` header is required.
CSRF protection is disabled for local learning and must be restored before production use with cookie-based authentication.

For authorization tests: missing/wrong credentials produce 401; authenticated USER writing inventory produces 403.
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

Discount statuses remain `AVAILABLE` / `USED`.
The supplied schema defines `used_at` as a nullable datetime: null means unused, and checkout records the consumption time.
Address and payment entities now match the supplied schema, which has no owner column on either table.
Their IDs are still accepted by checkout, with existence enforced by the database foreign keys.

## Order tracking

`GET /api/orders/tracking/{id}` accepts the order ID and returns `TrackingOrderDetailResponse` inside `BaseResponse.data`.
The response contains the order ID, current order state, purchased items, stored total, shipping address, and payment method.
Each item's price is the unit price saved on the order item, and its description is the product variant name.
An absent payment returns null, and an order without active items returns an empty list.
The caller must have the `USER` role and own the order.
The service resolves the current user; both repository queries enforce ownership and exclude deleted orders and items.
Missing, deleted, and other users' orders all return 404 with `Order not found`.
Anonymous requests return 401, and callers without the `USER` role return 403.
Possession of an order ID does not grant access.
