# Estimated delivery

Run `docs/sql/address-city.sql` once against MySQL before starting the updated app (`ddl-auto: validate` does not migrate the schema).
Backfill `addresses.city` with the city name, for example `Hà Nội` or `Hồ Chí Minh`.
Legacy addresses may have a null city; status updates do not require a city.
No new address-management endpoint is added.

## Start processing

An authenticated ADMIN sends `PATCH /api/orders/tracking/{orderId}/state` with the session cookie or HTTP Basic credentials and this body:

```json
{"state":"PROCESSING"}
```

Status updates preserve `orders.estimated_delivery`, including null values and cancellation.
They update only the order state and audit fields inside a write transaction.
Supported progression is PENDING -> CONFIRMED (optional) -> PROCESSING -> SHIPPING -> DELIVERED; cancellation is allowed before SHIPPING.
The target state must already exist in `order_states`.
Unknown or missing request states return 400, invalid transitions return 400, missing/deleted orders or target states return 404, and USER accounts cannot update states.

The state-update API returns a success response with `data: null`, including on same-state retries.
Read the estimated date and remaining days through the tracking API instead of a separate delivery response.

## Read tracking

The owning USER calls `GET /api/orders/tracking/{orderId}` as before.
The response's `data` contains all nine tracking fields:

```json
{
  "orderTrackingId": "order-id",
  "orderTrackingStatus": "PROCESSING",
  "purchasedItems": [
    {
      "purchasedItemName": "Tea",
      "purchasedItemDescription": "Large",
      "purchasedItemQuantity": 2,
      "purchasedItemPrice": 9.75
    }
  ],
  "totalAmount": 30019.50,
  "shippingAddress": "123 Test Street",
  "paymentMethodInfo": "CASH",
  "shippingCity": "Hà Nội",
  "estimatedDelivery": "2026-09-21",
  "daysRemaining": 1
}
```

`estimatedDelivery` is a Java `LocalDate`, serialized as `yyyy-MM-dd`; `daysRemaining` is an `Integer`.
Tracking reads `estimatedDelivery` directly from `orders.estimated_delivery` and does not calculate a replacement date.
If the saved date is missing for a nonterminal order, both estimate fields are null, even when its city is available.
`daysRemaining` is the number of days from today in `Asia/Ho_Chi_Minh` to the returned date, clamped at zero.
Delivered orders return zero days and keep their saved date without inventing a new future estimate.
Cancelled orders return null for both estimate fields while retaining the saved date in the database.

The query's null placeholders are filled in the service: `purchasedItems` comes from `order_items` joined to `product_variants` and `products`, and `daysRemaining` is calculated from the date.
Item quantities and unit prices come from the saved order items, not the current catalog price.
Checkout already calculates and saves each `line_total = unit_price × quantity`, the subtotal as their sum, and the final total after discount and shipping.
Tracking reads the saved final total as `totalAmount`; individual line totals are not currently exposed in `PurchasedItemResponse`.
