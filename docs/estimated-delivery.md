# Estimated delivery

Run `docs/sql/address-city.sql` once against MySQL before starting the updated app (`ddl-auto: validate` does not migrate the schema).
Backfill `addresses.city` with the city name, for example `Hà Nội` or `Hồ Chí Minh`.
Legacy addresses may remain null, but an admin cannot start processing their orders until city is supplied.
No new address-management endpoint is added.

`DeliveryZone` holds the fixed shipping policy:
- `CAPITAL`: Hà Nội, Hồ Chí Minh - 1 calendar day.
- `NEARBY`: Bắc Ninh, Hải Phòng, Đồng Nai, Tây Ninh - 2 calendar days (provisional business list, not a distance calculation).
- `DISTANT`: any other nonblank city - 3 calendar days.

Matching ignores accents, capitalization and extra spaces.
Change the nearby list in the enum if your delivery policy differs.

## Start processing

An authenticated ADMIN sends `PATCH /api/orders/tracking/{orderId}/state` with the session cookie or HTTP Basic credentials and this body:

```json
{"state":"PROCESSING"}
```

The estimate is the processing-start date plus shipping days, using `Asia/Ho_Chi_Minh` calendar dates.
For example, an order placed September 18 and processed September 20 for Hà Nội is estimated to arrive September 21.
Do not add the waiting time again to the processing-start date.
Retries and subsequent SHIPPING updates preserve the saved estimate.
Supported progression is PENDING -> CONFIRMED (optional) -> PROCESSING -> SHIPPING -> DELIVERED; cancellation is allowed before SHIPPING.
The target state must already exist in `order_states`.
Unknown or missing request states return 400, invalid transitions return 400, missing/deleted orders or addresses return 404, and USER accounts cannot update states.

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
The saved `orders.estimated_delivery` takes priority.
If it is missing for a nonterminal order with a city, the service returns a provisional date of today plus `DeliveryZone` shipping days.
This fallback is recalculated on each read and does not write to the database; entering PROCESSING still saves the processing-start estimate.
`daysRemaining` is the number of days from today to the returned date, clamped at zero.
Delivered orders return zero days and keep their saved date without inventing a new future estimate.
Cancelled orders return null for both estimate fields; orders missing both a saved date and city also have no estimate.

The query's null placeholders are filled in the service: `purchasedItems` comes from `order_items` joined to `product_variants` and `products`, and `daysRemaining` is calculated from the date.
Item quantities and unit prices come from the saved order items, not the current catalog price.
Checkout already calculates and saves each `line_total = unit_price × quantity`, the subtotal as their sum, and the final total after discount and shipping.
Tracking reads the saved final total as `totalAmount`; individual line totals are not currently exposed in `PurchasedItemResponse`.
