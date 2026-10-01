# order_services

Spring Boot backend for carts, checkout, inventory, order tracking, returns, and shipper delivery. The flow is controller → service → Spring Data JPA repository → database. JSON APIs use `BaseResponse`; return exports download CSV directly.

## Requirements and setup

- JDK 25, Spring Boot 4.1.1, and the Maven wrapper (Maven 3.9.16).
- MySQL with an existing compatible `order_services` schema. The repository does not include a complete schema bootstrap.
- Configure the datasource before starting. `src/main/resources/application.yaml` points to local MySQL and contains development-only credentials. Override them with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; keep real credentials outside Git.

```sh
export JAVA_HOME="/path/to/jdk-25"
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/order_services"
export SPRING_DATASOURCE_USERNAME="your-local-user"
export SPRING_DATASOURCE_PASSWORD="your-local-password"
./mvnw spring-boot:run
```

HTTP uses port 8080 by default; override with `SERVER_PORT`. Hibernate uses `ddl-auto: validate`: it checks existing tables but does not create or migrate them. SQL logging is enabled in the current development configuration.

### Database changes

Review each script against the current database before applying it manually:

- [address-city.sql](docs/sql/address-city.sql): adds the city used in tracking.
- [order-notifications.sql](docs/sql/order-notifications.sql): updates the existing notifications table for the notification worker.
- [order-return-export-indexes.sql](docs/sql/order-return-export-indexes.sql): indexes for large return exports.

These are incremental scripts, not a fresh-database installation. The current delivery entities also require `orders.shipper_id`, `orders.assigned_at`, `orders.delivery_attempt_id`, and `tracking_logs.recipient_name`, `tracking_logs.failure_reason`, `tracking_logs.delivery_attempt_id`; corresponding migration scripts are not supplied here. Compare the existing database with the entities before running delivery features.

## Build and verification

```sh
./mvnw -DskipTests compile
./mvnw test
./mvnw package
java -jar target/Order_Services-0.0.1-SNAPSHOT.jar
```

Use `mvnw.cmd` on Windows. Java compilation is the available type check; no standalone lint or formatter task is configured. Integration tests use H2 rather than the application's runtime MySQL connection, so passing tests do not validate the existing MySQL schema.

For a focused return/export check:

```sh
./mvnw -Dtest=OrderReturnsIntegrationTest,OrderReturnFilterTest test
```

Full tests are not required for every edit; choose checks appropriate to the change. Checkout rollback/concurrency tests describe a deferred transaction requirement and may fail with the current implementation; do not describe the entire suite as green without running it.

## Authentication

Use `POST /api/auth/login` with email and password, then retain the `JSESSIONID` cookie. HTTP Basic is also supported, with email as its username. Authentication uses BCrypt and sessions, not JWT. Roles are `USER`, `ADMIN`, and `SHIPPER`; a shipper does not automatically inherit other roles.

Services resolve the acting account from the authenticated email. Resource IDs in requests do not replace ownership or assignment checks. Anonymous protected requests return 401; authenticated callers without the required role return 403.

CSRF protection is disabled for local learning. Restore it before deploying cookie-based authentication to production. See [login guide](docs/login-api.md) for session, role, and credential details.

## Current API routes

Paths below combine the controller-level and method-level mappings, including their current spelling and punctuation.

| Method | Route | Role |
| --- | --- | --- |
| POST | `/api/auth/login` | Public |
| GET | `/api/carts` | USER |
| PATCH | `/api/carts/items/{cartItemId}/quantity` | USER |
| GET | `/api/discounts` | USER |
| POST | `/api/orders/orders/summary` | USER |
| POST | `/api/orders` | USER |
| GET | `/api/orders/tracking/{id}` | USER, owner |
| GET | `/api/orders/returns/summary!` | ADMIN |
| GET | `/api/orders/returns` | ADMIN |
| GET | `/api/orders/returns/{id}` | ADMIN |
| GET | `/api/orders/return/export` | ADMIN |
| PATCH | `/api/orders/{id}/shipper` | ADMIN |
| PATCH | `/api/orders/tracking/{id}/state` | ADMIN |
| POST | `/api/products` | ADMIN |
| POST | `/api/products/{productId}/variants` | ADMIN |
| GET | `/api/inventories` | ADMIN |
| PUT | `/api/inventories/{productVariantId}/quantity` | ADMIN |
| GET | `/api/inventories/products-in-stock` | ADMIN |
| PATCH | `/api/inventories/productsInStock/{productId}/variants/{variantsId}` | ADMIN |
| POST | `/api/shipper/orders/receive` | SHIPPER |
| POST | `/api/shipper/orders/delivered` | SHIPPER |
| POST | `/api/shipper/orders/failed` | SHIPPER |

The repeated `/orders` in the summary route and the trailing `!` in the return-summary route are present in the current controller, not documentation typos. This documentation update does not change those routes.

### Checkout and tracking

Money is calculated server-side with `BigDecimal`. Shipping is currently fixed at `30000.00`; discounts apply to the product subtotal. Tracking reads saved order prices, totals, and estimated delivery dates rather than recomputing them from catalog prices.

**Current limitation:** checkout's write transaction is deferred. Do not rely on checkout being atomic or use it for real orders until transaction, stock locking, and rollback behavior are verified. Assigned-discount dates are not currently checked. Address and payment IDs rely on existence/foreign-key validation; the mapped tables have no owner column.

- [Checkout and cart guide](docs/checkout-api.md)
- [Tracking and estimated delivery guide](docs/estimated-delivery.md)
- [Order returns and CSV export guide](docs/order-returns-api.md)

### Shipper delivery

Admins assign an active SHIPPER with `PATCH /api/orders/{id}/shipper` when the order is `PROCESSING` or `DELIVERY_FAILED`. A new or changed shipper assignment creates a delivery-attempt token returned with the delivery response; assigning the same shipper again preserves the token. Keep the current `deliveryAttemptId`; assigned-order updates validate it.

Shippers submit the order ID to `/api/shipper/orders/receive`; the service uses the current assigned attempt. `/api/shipper/orders/delivered` and `/api/shipper/orders/failed` also require the attempt token in the request. Receive moves an assigned `PROCESSING` order to `SHIPPING`. Delivery outcomes check the authenticated shipper's assignment, attempt token, recipient name against the customer's name, and shipping address. Failed outcomes also require a failure reason. See the request DTOs under `src/main/java/com/example/order_services/dto/request/` for exact fields and allowed values.

Admin state changes use `PATCH /api/orders/tracking/{id}/state`. Delivery retries are admin-controlled; changing the client-supplied shipper ID or using an old token does not authorize an outcome.

### Notifications

Transitions into `SHIPPING` or `DELIVERED` queue notification records alongside the state/tracking changes in the same write transaction. The scheduled worker starts after five seconds and runs with a five-second fixed delay, processing up to 100 pending records with fewer than three attempts.

This is a logging demo: it logs notification payloads and marks them processed. It does not send email or SMS. Review logged customer data and replace the demo sender before production use.

## Documentation for agents

Start with [AGENTS.md](AGENTS.md). Task-specific workflow references are in [docs/agents](docs/agents/): [GitHub issues](docs/agents/issue-tracker.md), [triage roles](docs/agents/triage-labels.md), and [domain documentation](docs/agents/domain.md).

Feature specs under `.scratch/` and plans/designs under `docs/superpowers/` record earlier requirements. They can disagree with the current implementation; check the requested spec against code and report conflicts instead of treating historical documents as the current API contract.
