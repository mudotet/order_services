# Simple estimated delivery

Status: Implemented with provisional nearby-city policy and processing-start anchor.

- Store city text on addresses, leaving legacy addresses nullable until backfilled.
- Fixed shipping policy: Hà Nội and Hồ Chí Minh take 1 calendar day; provisional nearby list Bắc Ninh, Hải Phòng, Đồng Nai and Tây Ninh takes 2; all other nonblank cities take 3.
- Admin-only `PATCH /api/orders/tracking/{id}/state` accepts an enum state and returns success with null data, including same-state retries.
- Return the estimated date and days remaining only through TrackingOrderDetailResponse, without a separate delivery DTO.
- Set `estimated_delivery` when first moving into PROCESSING, using that date in Asia/Ho_Chi_Minh plus shipping days.
- Time spent waiting before processing is included naturally; there is no extra fixed processing-day allowance.
- Use the saved estimate first; nonterminal orders without one get a response-only fallback of today plus DeliveryZone shipping days when their city is available.
- Retrying the same state is a no-op; later states do not move the estimate.
- Allow PENDING to CONFIRMED or PROCESSING, CONFIRMED to PROCESSING, PROCESSING to SHIPPING, and SHIPPING to DELIVERED.
- Allow cancellation before SHIPPING, but never reopen terminal states or move backwards.
- Require an active address and nonblank city before entering PROCESSING.
- Owner-only tracking keeps all nine response fields, with LocalDate estimatedDelivery and Integer daysRemaining calculated from the returned date, clamped at zero.
- DELIVERED has zero days remaining; CANCELLED clears the estimate and remaining days.
- Use the existing controller-service-repository flow with no geocoding, business-day calendar, scheduler or additional dependency.

## Verification

Both focused delivery HTTP tests pass, covering 1/2/3-day shipping, pending estimates, retries, overdue dates, delivery, cancellation, city validation and admin authorization.
The full suite ran 46 tests, with 6 failures and 3 errors from existing checkout issues and the concurrent change that makes tracking require a payment.
The API inventory assertion now includes the added PATCH route; its existing checkout-summary route mismatch remains unchanged.
Standards and spec reviews found no delivery-specific issue beyond that concurrent payment-join change, which is preserved pending user confirmation.
