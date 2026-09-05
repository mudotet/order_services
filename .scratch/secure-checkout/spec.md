# Service-owned identity and checkout correction

Status: Discount lookup simplified; checkout write transaction deferred by user request

The user's September 5 request supersedes the original no-authentication scope in
`docs/superpowers/specs/2026-08-25-order-service-apis-design.md`. The current JPA relationships,
per-user discounts and HTTP Basic authentication are retained. After restoring the original five
business APIs, the user explicitly requested one cart-item plus/minus API. The current scope is six
business APIs; the extra CSRF-token endpoint remains removed.

## Contract

- Exactly six routes: GET `/api/carts`, GET `/api/discounts`, POST `/api/orders/summary`,
  POST `/api/orders`, PUT `/api/inventories/{productVariantId}/quantity`,
  PATCH `/api/carts/items/{cartItemId}/quantity`.
- Only `CurrentUserService` reads `SecurityContextHolder`. It resolves `Authentication.getName()`
  through an active-user lookup. Cart, discount and order services call it themselves.
- Controllers neither receive current-user IDs nor resolve principals. Old user-ID routes are removed.
- Service methods require `USER`; inventory changes require `ADMIN`.
- Cart PATCH accepts `quantityChange` of exactly 1 or -1 and returns the new quantity. GET cart
  includes `cartItemId`. Identity is still resolved in the service; only items from the current
  user's active cart are eligible. Zero soft-deletes the item; invalid steps return 400, missing,
  deleted or foreign items return 404. Increasing validates total variant demand against stock,
  including duplicate lines. Decreasing is allowed even after stock drops. Stock is not reserved.
- Summary reads the current user's active cart, current prices and optional eligible discount.
  It does not read/create orders, reserve stock, consume a discount or clear the cart.
- Create-order uses the same calculation. Percentage discounts round only at the monetary amount;
  fixed discounts are capped at subtotal. Shipping is hard-coded to 30000.00 in the shared checkout
  calculation. Total is subtotal minus discount plus shipping; discounts do not reduce shipping.
- Create-order accepts the user's final selected `discountId`, which may differ from the preview.
  Null or omitted means no discount. Client values/totals are never used; eligibility and amounts
  are checked again on creation and only the final discount is consumed.
- `loadCheckout(userId, cart, discountId)` uses one `findAvailableAssignment` lookup for both preview
  and creation. The query checks owner, soft deletion, status and usage. The service
  obtains `assignment.getDiscount()` and `discount.getDiscountValue()` and calculates with if/else.
  There is no `forUpdate` flag or separate locking query for the assignment.
- Discount date checks are explicitly deferred by the user. Neither listing nor checkout receives
  `now` or filters on `receivedAt`/`expiredAt`. The mapped date fields remain for later use;
  expired and future-dated assignments are allowed if the other conditions pass.
- Cart quantity changes retain their existing READ_COMMITTED transaction and cart lock.
  The user has deferred adding a write transaction to `createOrder` while learning. Its existing
  class-level read-only transaction and cart/inventory locking queries have not been changed;
  persistence, rollback and concurrent checkout must not be considered safe for real orders.
- Fetch joins load cart item product details; inventory reads use one batch. Queries exclude
  soft-deleted rows. No order-item re-query or per-line price query is needed.
- Basic authentication keeps CSRF protection through Spring Security's built-in `csrf.spa()`.
  Existing requests supply the `XSRF-TOKEN` cookie; writes require the matching `X-XSRF-TOKEN` header.

## Validation

JUnit/Mockito tests cover calculation, quantity boundaries, missing/foreign/used discounts
and stock failures. Spring Boot/MockMvc/H2 tests exercise the actual security filter chain,
service proxies, repository queries, concurrent checkout and database transaction rollback.
They also enforce exactly six API routes, discount replacement/removal after preview, cart +/-
boundaries, ownership, combined stock limits, soft deletion and concurrent quantity increments.
Discount integration checks confirm that dates are ignored in listing and preview, while used,
revoked and deleted assignments are still rejected.
H2 is test-scoped; it does not replace MySQL in application configuration.
The concurrent-checkout and order-rollback integration checks were already failing before the
discount simplification because the checkout write transaction was removed. They remain in the
suite; the user declined restoring that transaction for now.

## Remaining schema checks

- MySQL at the configured localhost address refuses connections. H2 verifies JPQL and behavior,
  not compatibility with the actual MySQL DDL or MySQL concurrency semantics.
- Until the existing status vocabulary is supplied, eligible `user_discounts.status` is assumed
  to be `AVAILABLE`; consumption sets `USED`. Other status values are rejected, not silently accepted.
- The existing boolean column `used_at` remains mapped as before, but the Java field is now `used`.
  Confirm the actual column type; a datetime column would require a different mapping.
- Address/payment entities and their user relationships are absent from this repo. Their IDs
  remain accepted as in the original contract; existence is delegated to existing database foreign
  keys. Ownership of these two resources is not yet checked and needs the real DDL.

## Naming decisions

Java types use UpperCamelCase; fields/methods use lowerCamelCase and methods describe their action.
There is no additional naming-standard document in this repo. Spring override names are preserved.

| Before | After / decision |
| --- | --- |
| `CustomiezUserDetailsService` | `CustomUserDetailsService` |
| `calLineTotal` | private `calculateLineTotal` |
| `findNameProduct` | private `buildProductName` (formats values, does not query) |
| `checkProductStockStatus` | private `calculateStockStatus` (no query per item) |
| `calculateCartItemSubTotal`, `calculateOrderItemSubTotal` | replaced by the shared checkout calculation; `subtotal` is one word |
| `findProductVariantByIdById` | removed unused malformed derived query; inherited `findById` remains |
| `findAllByProductVariant_IdInAndDeletedFalse` | `findAllByProductVariantIdInAndDeletedFalse`: requested camelCase name, same derived query; no ambiguous `productVariantId` property exists on `Inventory` |
| `OrderItemService` | removed unused order-item query interface after switching summary to cart |
| `order_id` parameter | removed with the existing-order summary route |
| `CartId` field | `cartId` (database column unaffected) |
| `Permissions` entity | singular `Permission`, same `permissions` table |
| boolean `usedAt` | `used`, same `used_at` column |
| `loadUserByUsername` | unchanged: required by Spring `UserDetailsService` |
| `findByUserNameAndDeletedFalse` | `UserName` matches the entity's `userName` property; underscores in nested derived queries are valid Spring Data traversal separators |

The unused `common.Role` enum and mapped `entity.Role` share a simple name; this is legal Java but
could be clearer as `RoleName` if the enum is used later. No behavior currently depends on that enum.
