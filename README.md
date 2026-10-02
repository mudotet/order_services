# Order Workspace

A full-stack order management project combining a **Spring Boot backend** with a **React and Tailwind CSS frontend** in one repository. It connects product inventory, customer carts, order tracking, returns, and shipper delivery workflows through role-based access.

Built as a hands-on engineering project: database pagination, batched CSV exports, transaction boundaries, pessimistic locking, stale-request protection, SQL indexing, and targeted N+1 query mitigation.

> **Project status:** development and demonstration. The frontend can run without the backend using a development-only demo account. Real checkout is not production-ready: its inherited read-only transaction still requires correction and concurrency/rollback verification.

## Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Getting started](#getting-started)
- [Engineering skills demonstrated](#engineering-skills-demonstrated)
- [API overview](#api-overview)
- [Testing and verification](#testing-and-verification)
- [Known limitations](#known-limitations)
- [Further documentation](#further-documentation)

## Features

| Role | Capabilities |
| --- | --- |
| Administrator | View paginated orders, update order states, assign shippers, create products and variants, update prices and inventory, inspect return requests, and export returns as CSV. |
| Customer | Browse the catalog, add products to a cart, adjust quantities, preview server-calculated totals, submit checkout requests, and track personal orders. |
| Shipper | View assigned orders, receive deliveries, and report successful or failed delivery outcomes with delivery-attempt validation. |

The frontend includes a cinematic welcome page, GSAP scroll effects, responsive navigation, light/dark workspace themes, accessible form labels, loading/empty/error states, and a clearly marked demo mode. Real authentication uses session cookies, not frontend mock credentials or JWT.

## Screenshots

The tracked [`docs/images/`](docs/images/) directory is ready for your screenshots. No screenshots are fabricated or included yet.

Suggested filenames:

| File | What to capture |
| --- | --- |
| `welcome.png` | Welcome page and hero |
| `dashboard.png` | Administrator overview |
| `orders.png` | Order list and details |
| `inventory.png` | Product and stock management |
| `returns.png` | Return requests |
| `customer.png` | Customer catalog or cart |
| `shipper.png` | Assigned deliveries |
| `mobile.png` | Responsive mobile interface |

After adding the files, place the following Markdown in this section to display them on GitHub:

```markdown
### Welcome
![Welcome page](docs/images/welcome.png)

### Administrator workspace
![Administrator dashboard](docs/images/dashboard.png)
![Order management](docs/images/orders.png)
![Inventory management](docs/images/inventory.png)

### Customer and shipper workflows
![Customer interface](docs/images/customer.png)
![Shipper interface](docs/images/shipper.png)
![Mobile interface](docs/images/mobile.png)
```

Use demo data when capturing screenshots; do not publish customer information, passwords, or session cookies.

## Technology stack

| Layer | Technologies |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Spring Security, Spring Data JPA, Hibernate, Bean Validation, Lombok |
| Database | MySQL for runtime; H2 for integration tests |
| Frontend | React 19, JavaScript/JSX, Tailwind CSS 4, Vite 6 |
| Design | GSAP, ScrollTrigger, Phosphor icons, self-hosted Geist fonts |
| Verification | Maven wrapper, JUnit/Mockito, ESLint, TypeScript JavaScript checking, Node.js test runner |

## Architecture

```text
order_services/
├── src/main/java/com/example/order_services/
│   ├── controller/          # HTTP endpoints and role restrictions
│   ├── service/             # Business logic and transactions
│   ├── repository/          # JPA queries, projections, and locks
│   ├── entity/              # Persistence mappings and audit fields
│   └── dto/                 # Request validation and response contracts
├── src/main/resources/      # Backend configuration
├── src/test/                # Backend unit and integration tests
├── frontend/
│   ├── src/                 # React UI, API client, and development demo
│   ├── tests/               # API-client and demo checks
│   └── package.json
├── docs/
│   ├── images/              # Add screenshots here
│   └── sql/                 # Manual incremental database scripts
├── pom.xml
├── mvnw
└── README.md
```

Request flow: **React → JSON API → controller → service → repository → database**.

Most JSON endpoints return `{code, message, data, metadata}`. CSV exports return a downloadable file directly. Services derive the acting user from the authenticated email and enforce ownership or shipper assignment; a client-supplied resource ID is not authorization. Monetary calculations use server-side `BigDecimal`.

## Getting started

### Prerequisites

- JDK **25** for the configured backend build.
- Node.js **22 LTS** and npm recommended for frontend tooling.
- MySQL with an existing compatible `order_services` schema.
- Git; Maven is supplied through the wrapper.

### Frontend-only demo

From the repository root:

```sh
npm --prefix frontend ci
npm --prefix frontend run dev
```

Open `http://localhost:5173`, select **Vào không gian**, and sign in:

| Field | Value |
| --- | --- |
| Email | `demo@order.local` |
| Password | `Demo123!` |

The development login form is prefilled. The demo includes administrator, customer, and shipper roles with isolated sample data. Mutations remain in browser memory and reset on reload; a separate session-storage flag remembers the demo login for the current browser tab.

Demo API interception is enabled **only in Vite development mode**. Production builds use real backend authentication. These credentials do not create an account in MySQL.

### Backend configuration

Set environment variables in the terminal that runs the backend:

```sh
export JAVA_HOME="/path/to/jdk-25"
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/order_services"
export SPRING_DATASOURCE_USERNAME="your-local-user"
export SPRING_DATASOURCE_PASSWORD="your-local-password"
./mvnw spring-boot:run
```

Use `mvnw.cmd` on Windows. The backend listens on port **8080** by default; override with `SERVER_PORT`. The datasource password must be supplied through the environment. Keep real credentials outside Git. Spring Boot does not automatically load frontend `.env` files as backend environment variables.

Real login requires users and role assignments in the existing database, with BCrypt password hashes. There is no registration endpoint or complete seed/bootstrap procedure in this repository.

### Database preparation

Hibernate uses `ddl-auto: validate`: it validates mappings but does not create or migrate tables. The repository does **not** contain a complete fresh-database bootstrap.

Review and apply these incremental scripts manually against your existing schema:

- [Address city](docs/sql/address-city.sql)
- [Notification changes and index](docs/sql/order-notifications.sql)
- [Return export indexes](docs/sql/order-return-export-indexes.sql)

Delivery entities additionally require `orders.shipper_id`, `orders.assigned_at`, `orders.delivery_attempt_id`, and `tracking_logs.recipient_name`, `tracking_logs.failure_reason`, `tracking_logs.delivery_attempt_id`. Their migration scripts are not supplied; compare the database with the current entities before using delivery features.

### Connect the frontend to the backend

Run the backend and frontend in separate terminals. Vite proxies `/api` to `http://localhost:8080`, keeping browser requests on the frontend origin and forwarding session cookies.

For a different backend address:

```sh
API_TARGET=http://localhost:8081 npm --prefix frontend run dev
```

For production, serve the built frontend and reverse-proxy `/api` under the same origin. A separately hosted cross-origin frontend requires an explicit credential-aware CORS allowlist; none is configured here.

```sh
npm --prefix frontend run build
npm --prefix frontend run preview
```

Preview serves the frontend build; it is not a production API reverse proxy and does not enable demo login.

## Engineering skills demonstrated

### Large-dataset handling with pagination

- **Database-level pagination:** order and return lists use Spring Data `Page`/`PageRequest`, rather than downloading every record and slicing it in the browser. Page sizes are validated between 1 and 100; inventory search currently uses a fixed size of four.
- **Stable ordering:** interactive listings sort by creation time and ID, giving equal timestamps a deterministic tie-breaker.
- **Keyset pagination for exports:** return CSV generation reads up to 500 parent return rows at a time using `id < lastSeenId` and descending ID order. This avoids progressively increasing offsets and total-count queries during export.
- **Bounded parent batches:** DTO projections and a bulk reason lookup per batch avoid retaining the entire exported entity graph in memory. The generated file is written to temporary disk storage, then downloaded and cleaned up.

**Scope:** these are practical large relational-dataset techniques, not evidence of distributed big-data processing or measured million-row scalability. UUID export ordering is deterministic, not chronological. Interactive offset pagination can become expensive on deep pages; exports are synchronous and require sufficient request time and disk space. Catalog reads and some summary calculations remain unbounded.

### Race-condition handling and transaction design

- **Pessimistic write locks:** cart mutations and inventory updates acquire database row locks inside write transactions, reducing lost updates during concurrent requests.
- **Consistent lock ordering:** multi-variant inventory lock queries request variant-ID ordering to reduce deadlock risk.
- **Delivery state consistency:** order assignment and delivery transitions lock the order. Order state, tracking history, and applicable notification records are persisted in the same write transaction.
- **Stale-request protection:** reassignment or delivery retry creates a new `deliveryAttemptId`; outdated attempts are rejected with a conflict instead of updating the current delivery.
- **Authorization and state validation:** delivery actions verify the authenticated shipper, current assignment, allowed state transition, recipient, address, and attempt token.

Delivery-attempt tokens are application-level guards alongside pessimistic locking, **not** JPA `@Version` optimistic locking. These controls address specific races; they do not eliminate every race or deadlock.

**Known checkout defect:** `createOrder` currently inherits a class-level read-only transaction without a write override. Calling locking repositories alone does not prove correct persistence, atomicity, or rollback. Checkout must be fixed and verified before accepting real orders.

### SQL query and index optimization

- **DTO/scalar projections:** tracking, export, and aggregate queries retrieve the required fields rather than unnecessarily materializing complete object graphs.
- **Bulk lookups:** cart inventory and return-item relationships are loaded for a set of IDs rather than through one query per record.
- **Database-side aggregation:** selected inventory and return summaries use aggregate repository queries.
- **Composite index scripts:** return exports provide `(deleted, id DESC)` and `(deleted, status, id DESC)` indexes aligned with soft-deletion filters, optional status filtering, and cursor ordering. Notification polling provides `(processed_at, deleted, created_at)`.

The scripts must be applied to the actual database; their presence does not establish deployed indexes or measured speedups. Substring search uses `LOWER(...) LIKE '%...%'`, which generally cannot benefit from ordinary B-tree prefix/range lookup. Verify actual plans with MySQL `EXPLAIN` and representative data before claiming performance results.

### Targeted N+1 query mitigation

N+1 occurs when fetching a list triggers additional relationship queries for each record. The project addresses it on selected paths:

| Read path | Mitigation |
| --- | --- |
| Cart details | Fetch joins load cart items with variants/products; inventory is retrieved once for all variant IDs. |
| Inventory search | Fetch joins load variant/product relationships; the paginated query supplies a separate count query. |
| Return lists | Entity graphs fetch order/customer data, followed by a bulk item lookup for the selected return IDs. |
| Return export | DTO projections and one bulk reason lookup per batch avoid per-return reason queries. |
| Tracking items | Constructor projections retrieve purchased-item response fields directly. |

**Remaining work:** the order-browsing DTO mapper still queries addresses and items per order and traverses lazy relationships. Query count can grow with page size. This is a documented optimization opportunity, not a claim that N+1 has been eliminated project-wide. SQL logging is enabled for development; query-count tests and real execution-plan measurements would strengthen verification.

### Frontend and security engineering

- Role-specific workspaces backed by server-side role and ownership checks.
- Session restoration and logout with cookie invalidation; `credentials: 'include'` in the API client.
- Backend business-error envelopes handled alongside non-JSON security errors.
- React state-driven forms, search, filters, pagination, and detail dialogs.
- Native dialog accessibility, visible focus states, reduced-motion support, and responsive layouts.
- GSAP context cleanup and a development-only mock API that does not authorize production requests.

## API overview

Roles are independent: `ADMIN` does not automatically inherit `USER` or `SHIPPER` permissions.

| Area | Endpoints | Access |
| --- | --- | --- |
| Authentication | `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` | Login public; session endpoints authenticated |
| Catalog and checkout options | `GET /api/catalog`, `/api/addresses`, `/api/payments`, `/api/discounts` | USER |
| Cart | `GET /api/carts`, `POST /api/carts/items`, `PATCH /api/carts/items/{cartItemId}/quantity` | USER |
| Checkout | `POST /api/orders/orders/summary`, `POST /api/orders` | USER |
| Personal orders | `GET /api/orders/mine`, `GET /api/orders/tracking/{id}` | USER; owner-scoped |
| Administration | `GET /api/orders`, `GET /api/orders/{id}`, `GET /api/shippers` | ADMIN |
| Delivery assignment/state | `PATCH /api/orders/{id}/shipper`, `PATCH /api/orders/tracking/{id}/state` | ADMIN |
| Products | `POST /api/products`, `POST /api/products/{productId}/variants` | ADMIN |
| Inventory | `GET /api/inventories`, `GET /api/inventories/products-in-stock`, `PUT /api/inventories/{productVariantId}/quantity` | ADMIN |
| Product editing | `PATCH /api/inventories/productsInStock/{productId}/variants/{variantsId}` | ADMIN |
| Returns | `GET /api/orders/returns/summary!`, `/api/orders/returns`, `/api/orders/returns/{id}`, `/api/orders/return/export` | ADMIN |
| Shipper reads | `GET /api/shipper/orders`, `GET /api/shipper/orders/{id}` | SHIPPER; assignment-scoped |
| Shipper actions | `POST /api/shipper/orders/receive`, `/api/shipper/orders/delivered`, `/api/shipper/orders/failed` | SHIPPER; assignment and attempt checks |

The repeated `/orders` in the checkout summary and trailing `!` in the return summary are actual controller mappings.

Order lists accept `page`, `size`, `state`, and `query`. Return lists accept `page`, `size`, and `filterBy`. Inventory uses `query` and `page`. Pages are zero-based. See request/response DTOs for exact contracts.

## Testing and verification

### Frontend

```sh
npm --prefix frontend ci
npm --prefix frontend run lint
npm --prefix frontend run typecheck
npm --prefix frontend test
npm --prefix frontend run build
```

The typecheck uses TypeScript to check JavaScript/JSX without converting the application to TypeScript. Node tests cover API envelopes, session-cookie options, demo credentials, stock/cart changes, and demo delivery contracts.

### Backend

```sh
./mvnw -DskipTests compile
./mvnw -Dtest=CartServiceTest,InventoryServiceTest,AuthIntegrationTest test
./mvnw -Dtest=OrderReturnsIntegrationTest,OrderReturnFilterTest test
./mvnw test
./mvnw package
java -jar target/Order_Services-0.0.1-SNAPSHOT.jar
```

Java compilation is the backend type check; there is no standalone backend lint task. Integration tests use H2, so they do not validate the runtime MySQL schema, lock behavior, or execution plans. The full suite is not certified green; checkout rollback/concurrency tests describe a deferred transaction requirement and may fail with the current implementation.

## Known limitations

- Checkout inherits a read-only transaction and is not suitable for real orders until write/rollback/concurrency behavior is corrected and tested.
- The repository lacks a complete schema bootstrap and some delivery migrations.
- Address/payment entities lack ownership columns. Lookup endpoints expose only records referenced by the current user's existing orders, so a first-time customer can have no checkout options. Checkout itself still relies on existence/foreign-key validation of IDs.
- Discount assignment dates are not currently validated.
- CSRF protection is disabled for local learning. Restore appropriate protection before deploying cookie-based authentication.
- Notification processing logs payloads instead of sending email/SMS. Its synchronization is limited to one JVM; multiple instances need database-backed claim/idempotency handling.
- Selected read paths still have unbounded reads or N+1 risks; no load-test or SQL benchmark results are claimed.
- Demo changes reset on reload; demo credentials are disabled in production builds.

## Further documentation

- [Authentication](docs/login-api.md)
- [Checkout and carts](docs/checkout-api.md)
- [Tracking and estimated delivery](docs/estimated-delivery.md)
- [Returns and CSV export](docs/order-returns-api.md)
- [Agent conventions](AGENTS.md)
- [Agent workflow references](docs/agents/)

Specs under `.scratch/` and plans under `docs/superpowers/` are historical. Verify their requirements against current controllers, services, entities, and tests rather than treating them as the current API contract.
