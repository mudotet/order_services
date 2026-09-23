# Simple estimated delivery

Status: Implemented using the saved order date, with only remaining days calculated.

- Store city text on addresses, leaving legacy addresses nullable until backfilled.
- Admin-only `PATCH /api/orders/tracking/{id}/state` accepts an enum state and returns success with null data, including same-state retries.
- Return the estimated date and days remaining only through TrackingOrderDetailResponse, without a separate delivery DTO.
- Status updates preserve `estimated_delivery`, including null values and cancellation, and persist state and audit changes in a write transaction.
- Read the saved estimate directly; nonterminal orders without one return null for both estimate fields, regardless of city.
- Retrying the same state is a no-op; later states do not move the estimate.
- Allow PENDING to CONFIRMED or PROCESSING, CONFIRMED to PROCESSING, PROCESSING to SHIPPING, and SHIPPING to DELIVERED.
- Allow cancellation before SHIPPING, but never reopen terminal states or move backwards.
- Status updates do not require a city or calculate a delivery date.
- Owner-only tracking keeps all nine response fields, with LocalDate estimatedDelivery and Integer daysRemaining calculated from the returned date, clamped at zero.
- Calculate remaining days using today's date in Asia/Ho_Chi_Minh.
- DELIVERED has zero days remaining; CANCELLED hides the estimate and remaining days in tracking while preserving the saved date.
- Use the existing controller-service-repository flow with no geocoding, business-day calendar, scheduler or additional dependency.

## Verification

Focused HTTP tests cover saved and missing dates, retries, overdue dates, delivery, cancellation, missing city, persisted status updates and admin authorization.
