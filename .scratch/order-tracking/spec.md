# Order tracking information

Status: Implemented; tracking HTTP and repository checks pass.

Implement the user's existing `GET /api/orders/tracking/{id}` route through controller, service, and repository queries.
Reuse `TrackingOrderDetailResponse` and `PurchasedItemResponse`.
Return current order state, purchased items with their saved unit prices, stored total, shipping address, and optional payment method.
Resolve identity through `CurrentUserService`.
Require the `USER` role and restrict both queries to the authenticated owner's active order.
Use identical 404 responses for missing, deleted, and other users' orders.
Keep deleted order items out of the purchased-item list.
Return an empty item list and null payment information when appropriate.
Do not add tracking-log history or new write APIs.

The focused H2 schema test covers repository projections and ownership filters.
The HTTP integration test covers owner success, another user supplying the order ID, absent orders, deleted orders, anonymous access, and role restrictions.
The return-order implementation now compiles, and the tracking HTTP test passes.
The full suite still reports separate checkout routing, cart constraint, and transaction failures.
