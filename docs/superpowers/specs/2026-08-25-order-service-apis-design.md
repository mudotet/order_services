# Order Service APIs Design

## Goal

Implement five beginner-friendly APIs for inventory, cart, discounts, order summary, and order creation against the supplied `order_services` MySQL schema.

## Scope

Exactly these HTTP endpoints are exposed:

| Method | Path | Behaviour |
| --- | --- | --- |
| `PUT` | `/api/inventories/{productVariantId}/quantity` | Set the exact in-stock quantity. |
| `GET` | `/api/carts/{userId}` | Return the user's active cart, item details, and totals. |
| `GET` | `/api/discounts` | Return all active discounts. |
| `POST` | `/api/orders/summary` | Calculate the current cart total with an optional discount. |
| `POST` | `/api/orders` | Create an order from the current cart. |

Authentication, roles, notifications, generic CRUD endpoints, shipping-price calculation, and user-specific discount assignments are out of scope. Discounts are global because the database does not contain a user-discount relationship.

## Application Structure

Use the direct flow requested for this project:

```text
Controller -> Service -> Repository -> MySQL
```

- Controllers validate request DTOs and return `BaseResponse`.
- Services hold the business flow.
- Repositories use Spring Data JPA only for database access.
- ModelMapper converts simple entity values to response DTOs. A single `ModelMapper` bean lives in `OrderServicesApplication`; no mapper configuration layer is added.
- JPA entities use IDs instead of rich object relationships where an ID is all this feature needs. This avoids unnecessary bidirectional mappings.

### Shared Types

- `BaseEntity`: `id`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`, `deletedAt`, and `deleted`. It is a JPA mapped superclass that creates UUID string IDs and audit timestamps.
- `EnumCode`: `SUCCESS`, `BAD_REQUEST`, `NOT_FOUND`, and `INTERNAL_ERROR`, each with the response code/message used by `BaseResponse`.
- `BaseResponse<T>`: `code`, `message`, `data`, and `metadata`.
- `ApplicationException`: carries an `EnumCode` for expected business errors.
- `GlobalExceptionHandler`: converts `ApplicationException`, validation errors, database foreign-key violations, and unexpected errors to `BaseResponse`.

## Data Used

Only existing tables are mapped: `product_variants`, `inventories`, `carts`, `cart_items`, `discounts`, `order_states`, `orders`, and `order_items`. Address and payment IDs are accepted when creating an order; the existing database foreign keys verify that they refer to valid records.

All queries ignore soft-deleted rows. The default `PENDING` state is found by its `state` value, not a hard-coded UUID.

## Endpoint Contracts

### Update product quantity

`PUT /api/inventories/{productVariantId}/quantity`

```json
{ "quantity": 20 }
```

Find the active inventory record for the product variant, reject negative values, set `quantityInStock`, and return the updated inventory data.

### Retrieve cart details

`GET /api/carts/{userId}`

Return cart items with the product variant name, current `unitPrice`, `quantity`, `lineTotal`, and cart `subtotal`. If the user has no active cart, return an empty item list and zero totals.

### Fetch discounts

`GET /api/discounts`

Return active discounts with ID and percentage. Every active discount is available to every user for this first version.

### Calculate order summary

`POST /api/orders/summary`

```json
{ "userId": "user-id", "discountId": "optional-discount-id" }
```

Load the active cart and current product-variant prices. Calculate `subtotal`, percentage `discountAmount`, `shippingFee` as `0.00`, and `total = subtotal - discountAmount + shippingFee`. Reject an empty cart or an inactive/missing discount.

### Create an order

`POST /api/orders`

```json
{
  "userId": "user-id",
  "addressId": "address-id",
  "paymentId": "payment-id",
  "discountId": "optional-discount-id"
}
```

The service recalculates the summary; it never accepts prices or totals from the client. In one transaction it:

1. checks every cart item has enough stock;
2. creates an `orders` record with `PENDING` state and the calculated `discountAmount`;
3. creates `order_items` using the current price as the price snapshot;
4. reduces each inventory quantity; and
5. soft-deletes the cart items to clear the cart.

If any validation or stock check fails, no order or inventory change is committed.

## Error Handling

Missing inventory, product variant, discount, cart item, or order state returns `NOT_FOUND`. Negative quantities, empty carts, and insufficient stock return `BAD_REQUEST`. Request validation errors and foreign-key violations from invalid address or payment IDs also return `BAD_REQUEST`. Unhandled exceptions return `INTERNAL_ERROR` without exposing database details.

## Verification

Add one focused `OrderService` unit test using mocked repositories. It proves two cart lines with a discount produce the expected subtotal, `discountAmount`, and total, while successful order creation reduces stock and clears cart items. Run the Maven test suite after implementation.
