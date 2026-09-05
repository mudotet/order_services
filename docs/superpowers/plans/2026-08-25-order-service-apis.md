# Order Service APIs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the five approved order-service APIs with a beginner-friendly Spring MVC, service, repository, and JPA structure.

**Architecture:** Controllers validate DTOs and delegate to small services. Services use Spring Data repositories and return response DTOs wrapped in `BaseResponse`; order creation is the only transaction. Entities retain IDs instead of JPA object graphs, and ModelMapper handles direct entity-to-response mappings.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Bean Validation, MySQL, Lombok, ModelMapper 3.2.4, JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-08-25-order-service-apis-design.md`

## Global Constraints

- Expose exactly five HTTP endpoints listed in the spec; do not add CRUD, authentication, shipping, notification, or discount-assignment APIs.
- Use only the dependencies already in `pom.xml`; do not add a testing database or a mapping library.
- Keep the flow `Controller -> Service -> Repository -> MySQL`; no generic service/repository layer and no bidirectional JPA mappings.
- Ignore rows where `deleted = true`; the order state is found by `state = PENDING`.
- Recalculate all order money values on the server. The client never sends a price, subtotal, discount amount, or total.
- Do not commit during this plan: `git status` already has staged user changes. Never stage or commit those files without explicit user direction.

## File Structure

| Path | Responsibility |
| --- | --- |
| `common/BaseEntity.java` | Shared UUID, soft-delete, and audit fields for JPA entities. |
| `common/EnumCode.java` | Stable success and error codes with HTTP statuses. |
| `common/BaseResponse.java` | Uniform API envelope. |
| `exception/ApplicationException.java` | Expected business failure carrying an `EnumCode`. |
| `exception/GlobalExceptionHandler.java` | Maps expected, validation, foreign-key, and unexpected failures to the envelope. |
| `model/*.java` | Minimal JPA table mappings used by the five APIs. |
| `repository/*.java` | Queries for active rows only. |
| `dto/request/*.java` | Validated JSON bodies. |
| `dto/response/*.java` | Endpoint response shapes. |
| `service/*.java` | Inventory, cart, discount, and order use cases. |
| `controller/*.java` | The five route handlers. |
| `src/test/java/.../service/*.java` | Unit tests for the inventory and order business rules. |

### Task 1: Common HTTP and persistence foundation

**Files:**
- Create: `src/main/java/com/example/order_services/common/BaseEntity.java`
- Create: `src/main/java/com/example/order_services/common/EnumCode.java`
- Create: `src/main/java/com/example/order_services/common/BaseResponse.java`
- Create: `src/main/java/com/example/order_services/exception/ApplicationException.java`
- Create: `src/main/java/com/example/order_services/exception/GlobalExceptionHandler.java`
- Modify: `src/main/java/com/example/order_services/OrderServicesApplication.java`
- Delete: `src/test/java/com/example/order_services/OrderServicesApplicationTests.java`
- Test: `src/test/java/com/example/order_services/common/BaseResponseTest.java`

**Interfaces:**
- Produces: `BaseResponse.success(T data)`, `BaseResponse.error(EnumCode code, String message)`, and `new ApplicationException(EnumCode code, String message)`.
- Produces: a Spring `ModelMapper` bean for constructor injection into services.

- [ ] **Step 1: Write the failing response-envelope test.**

```java
@Test
void successResponseUsesSuccessCodeAndKeepsData() {
    BaseResponse<String> response = BaseResponse.success("saved");

    assertThat(response.getCode()).isEqualTo(EnumCode.SUCCESS.getCode());
    assertThat(response.getData()).isEqualTo("saved");
    assertThat(response.getMetadata()).isEmpty();
}
```

- [ ] **Step 2: Run the test to verify it fails.**

Run: `./mvnw -Dtest=BaseResponseTest test`

Expected: compilation fails because `BaseResponse` and `EnumCode` do not exist.

- [ ] **Step 3: Implement the shared types and handler.**

```java
// EnumCode.java
public enum EnumCode {
    SUCCESS("SUCCESS", "Request succeeded", HttpStatus.OK),
    BAD_REQUEST("BAD_REQUEST", "Invalid request", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "Resource not found", HttpStatus.NOT_FOUND),
    INTERNAL_ERROR("INTERNAL_ERROR", "Unexpected server error", HttpStatus.INTERNAL_SERVER_ERROR);
}

// BaseResponse.java
public static <T> BaseResponse<T> success(T data) {
    return new BaseResponse<>(EnumCode.SUCCESS.getCode(), EnumCode.SUCCESS.getMessage(), data, Map.of());
}

public static BaseResponse<Void> error(EnumCode code, String message) {
    return new BaseResponse<>(code.getCode(), message, null, Map.of());
}
```

`BaseEntity` is `@MappedSuperclass`, uses `String id`, and sets `id`, `createdAt`, `updatedAt`, and `deleted` in `@PrePersist`; `@PreUpdate` refreshes `updatedAt`. `GlobalExceptionHandler` has handlers for `ApplicationException`, `MethodArgumentNotValidException`, `DataIntegrityViolationException`, and `Exception`. The validation handler puts a `Map<String, String>` of field errors in `metadata`.

Add this bean to `OrderServicesApplication`:

```java
@Bean
ModelMapper modelMapper() {
    return new ModelMapper();
}
```

Remove the generated `@SpringBootTest` class because this project deliberately uses repository-mocked unit tests and must not require a local MySQL server merely to run `mvn test`.

- [ ] **Step 4: Run the test to verify it passes.**

Run: `./mvnw -Dtest=BaseResponseTest test`

Expected: `BUILD SUCCESS` and one passing test.

### Task 2: Minimal order-domain entities, repositories, and DTOs

**Files:**
- Create: `src/main/java/com/example/order_services/entity/ProductVariant.java`
- Create: `src/main/java/com/example/order_services/entity/Inventory.java`
- Create: `src/main/java/com/example/order_services/entity/Cart.java`
- Create: `src/main/java/com/example/order_services/entity/CartItem.java`
- Create: `src/main/java/com/example/order_services/entity/Discount.java`
- Create: `src/main/java/com/example/order_services/entity/OrderState.java`
- Create: `src/main/java/com/example/order_services/entity/OrderEntity.java`
- Create: `src/main/java/com/example/order_services/entity/OrderItem.java`
- Create: `src/main/java/com/example/order_services/repository/ProductVariantRepository.java`
- Create: `src/main/java/com/example/order_services/repository/InventoryRepository.java`
- Create: `src/main/java/com/example/order_services/repository/CartRepository.java`
- Create: `src/main/java/com/example/order_services/repository/CartItemRepository.java`
- Create: `src/main/java/com/example/order_services/repository/DiscountRepository.java`
- Create: `src/main/java/com/example/order_services/repository/OrderStateRepository.java`
- Create: `src/main/java/com/example/order_services/repository/OrderRepository.java`
- Create: `src/main/java/com/example/order_services/repository/OrderItemRepository.java`
- Create: `src/main/java/com/example/order_services/dto/request/UpdateQuantityRequest.java`
- Create: `src/main/java/com/example/order_services/dto/request/OrderSummaryRequest.java`
- Create: `src/main/java/com/example/order_services/dto/request/CreateOrderRequest.java`
- Create: `src/main/java/com/example/order_services/dto/response/InventoryResponse.java`
- Create: `src/main/java/com/example/order_services/dto/response/CartItemResponse.java`
- Create: `src/main/java/com/example/order_services/dto/response/CartDetailResponse.java`
- Create: `src/main/java/com/example/order_services/dto/response/DiscountResponse.java`
- Create: `src/main/java/com/example/order_services/dto/response/OrderSummaryResponse.java`
- Create: `src/main/java/com/example/order_services/dto/response/OrderResponse.java`

**Interfaces:**
- Consumes: `BaseEntity` from Task 1.
- Produces: repositories and DTOs used by Tasks 3–6.

- [ ] **Step 1: Add the entity mappings with only the required columns.**

```java
@Entity
@Table(name = "inventories")
public class Inventory extends BaseEntity {
    @Column(name = "product_variant_id", nullable = false, length = 36)
    private String productVariantId;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock;
}

@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {
    @Column(name = "product_variant", nullable = false)
    private String productVariant;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
}
```

Use these exact additional entity fields:

| Entity | Table | Fields besides `BaseEntity` |
| --- | --- | --- |
| `Cart` | `carts` | `String userId` |
| `CartItem` | `cart_items` | `String cartId`, `String productVariantId`, `Integer productQuantity` |
| `Discount` | `discounts` | `BigDecimal percentageDiscount` |
| `OrderState` | `order_states` | `String state` |
| `Order` | `orders` | `String userId`, `String discountId`, `String paymentId`, `String addressId`, `String orderStateId`, `BigDecimal subtotal`, `BigDecimal discountAmount`, `BigDecimal shippingFee`, `BigDecimal total` |
| `OrderItem` | `order_items` | `String orderId`, `String productVariantId`, `BigDecimal unitPrice`, `Integer quantity`, `BigDecimal lineTotal` |

All classes use Lombok `@Getter`, `@Setter`, and `@NoArgsConstructor`. Use `@Table(name = "orders")` for `Order`; never use a bare `Order` class name.

- [ ] **Step 2: Add active-row repository methods.**

```java
public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByUserIdAndDeletedFalse(String userId);
}

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findAllByCartIdAndDeletedFalse(String cartId);
}

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByProductVariantIdAndDeletedFalse(String productVariantId);
}
```

`ProductVariantRepository` and `DiscountRepository` each provide `findByIdAndDeletedFalse(String id)`; `DiscountRepository` also provides `findAllByDeletedFalse()`. `OrderStateRepository` provides `findByStateAndDeletedFalse(String state)`. `OrderRepository` and `OrderItemRepository` extend `JpaRepository` without custom methods.

- [ ] **Step 3: Add validated request and response DTOs.**

```java
public record UpdateQuantityRequest(@NotNull @Min(0) Integer quantity) {}
public record OrderSummaryRequest(@NotBlank String userId, String discountId) {}
public record CreateOrderRequest(
    @NotBlank String userId,
    @NotBlank String addressId,
    @NotBlank String paymentId,
    String discountId
) {}
```

`CartItemResponse` has `productVariantId`, `productVariant`, `unitPrice`, `quantity`, and `lineTotal`. `CartDetailResponse` has `userId`, `List<CartItemResponse> items`, and `subtotal`. `OrderSummaryResponse` has `subtotal`, `discountAmount`, `shippingFee`, and `total`. `OrderResponse` has `id`, `state`, and those four money fields. Every response DTO is a Lombok class with `@Getter`, `@Setter`, `@NoArgsConstructor`, and `@AllArgsConstructor` so ModelMapper can populate it; only request DTOs are records.

- [ ] **Step 4: Compile the domain layer.**

Run: `./mvnw -DskipTests compile`

Expected: `BUILD SUCCESS` with all entities and repository query names validated at compilation.

### Task 3: Inventory quantity API

**Files:**
- Create: `src/main/java/com/example/order_services/service/InventoryService.java`
- Create: `src/main/java/com/example/order_services/controller/InventoryController.java`
- Test: `src/test/java/com/example/order_services/service/InventoryServiceTest.java`

**Interfaces:**
- Consumes: `InventoryRepository`, `UpdateCartItemQuantityRequest`, `InventoryResponse`, `BaseResponse`, and `ApplicationException`.
- Produces: `InventoryService.updateQuantity(String productVariantId, UpdateQuantityRequest request)` and `PUT /api/inventories/{productVariantId}/quantity`.

- [ ] **Step 1: Write the failing service test.**

```java
@Test
void updateQuantitySetsTheExactQuantity() {
    Inventory inventory = new Inventory();
    inventory.setProductVariantId("variant-1");
    inventory.setQuantityInStock(3);
    when(inventoryRepository.findByProductVariantIdAndDeletedFalse("variant-1"))
        .thenReturn(Optional.of(inventory));

    InventoryResponse response = service.updateQuantity("variant-1", new UpdateQuantityRequest(12));

    assertThat(response.getQuantityInStock()).isEqualTo(12);
    verify(inventoryRepository).save(inventory);
}
```

- [ ] **Step 2: Run the test to verify it fails.**

Run: `./mvnw -Dtest=InventoryServiceTest test`

Expected: compilation fails because `InventoryService` does not exist.

- [ ] **Step 3: Implement the service and route.**

```java
public InventoryResponse updateQuantity(String productVariantId, UpdateQuantityRequest request) {
    Inventory inventory = inventoryRepository.findByProductVariantIdAndDeletedFalse(productVariantId)
        .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found"));
    inventory.setQuantityInStock(request.quantity());
    return modelMapper.map(inventoryRepository.save(inventory), InventoryResponse.class);
}

@PutMapping("/api/inventories/{productVariantId}/quantity")
public BaseResponse<InventoryResponse> updateQuantity(
        @PathVariable String productVariantId,
        @Valid @RequestBody UpdateQuantityRequest request) {
    return BaseResponse.success(inventoryService.updateQuantity(productVariantId, request));
}
```

- [ ] **Step 4: Run the focused test.**

Run: `./mvnw -Dtest=InventoryServiceTest test`

Expected: `BUILD SUCCESS` and one passing test.

### Task 4: Cart-details and discounts APIs

**Files:**
- Create: `src/main/java/com/example/order_services/service/CartService.java`
- Create: `src/main/java/com/example/order_services/service/DiscountService.java`
- Create: `src/main/java/com/example/order_services/controller/CartController.java`
- Create: `src/main/java/com/example/order_services/controller/DiscountController.java`
- Test: `src/test/java/com/example/order_services/service/CartServiceTest.java`

**Interfaces:**
- Consumes: Cart, cart-item, product-variant, and discount repositories from Task 2.
- Produces: `CartService.getCart(String userId)`, `DiscountService.getDiscounts()`, `GET /api/carts/{userId}`, and `GET /api/discounts`.

- [ ] **Step 1: Write the failing cart test.**

```java
@Test
void getCartReturnsAnEmptyCartWhenNoCartExists() {
    when(cartRepository.findByUserIdAndDeletedFalse("user-1")).thenReturn(Optional.empty());

    CartDetailResponse response = service.getCart("user-1");

    assertThat(response.getItems()).isEmpty();
    assertThat(response.getSubtotal()).isEqualByComparingTo("0.00");
}
```

- [ ] **Step 2: Run the test to verify it fails.**

Run: `./mvnw -Dtest=CartServiceTest test`

Expected: compilation fails because `CartService` does not exist.

- [ ] **Step 3: Implement the two read services and routes.**

```java
public CartDetailResponse getCart(String userId) {
    return cartRepository.findByUserIdAndDeletedFalse(userId)
        .map(this::toCartDetail)
        .orElseGet(() -> new CartDetailResponse(userId, List.of(), new BigDecimal("0.00")));
}

public List<DiscountResponse> getDiscounts() {
    return discountRepository.findAllByDeletedFalse().stream()
        .map(discount -> modelMapper.map(discount, DiscountResponse.class))
        .toList();
}
```

`toCartDetail` loads each active product variant, maps its name and price, and calculates `lineTotal = price.multiply(BigDecimal.valueOf(productQuantity))`; the subtotal is the sum of line totals. The cart controller returns `BaseResponse.success(cartService.getCart(userId))`; the discount controller returns `BaseResponse.success(discountService.getDiscounts())`.

- [ ] **Step 4: Run the focused test.**

Run: `./mvnw -Dtest=CartServiceTest test`

Expected: `BUILD SUCCESS` and one passing test.

### Task 5: Order-summary API

**Files:**
- Create: `src/main/java/com/example/order_services/service/OrderService.java`
- Modify: `src/main/java/com/example/order_services/controller/OrderController.java`
- Test: `src/test/java/com/example/order_services/service/OrderServiceTest.java`

**Interfaces:**
- Consumes: cart, cart-item, product-variant, discount, and order-state repositories; `OrderSummaryRequest`.
- Produces: `OrderService.calculateSummary(OrderSummaryRequest request)` and `POST /api/orders/summary`.

- [ ] **Step 1: Write the failing money-calculation test.**

Create `OrderServiceTest` with these fields, then add the test and helper methods below inside that class:

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductVariantRepository productVariantRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private OrderStateRepository orderStateRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private OrderService service;
}
```

```java
@Test
void calculateSummaryAppliesDiscountToCurrentCartPrices() {
    stubCart("user-1", item("variant-a", 2), item("variant-b", 1));
    stubVariant("variant-a", "Tea", "10.00");
    stubVariant("variant-b", "Cake", "5.00");
    stubDiscount("discount-1", "10.00");

    OrderSummaryResponse response = service.calculateSummary(new OrderSummaryRequest("user-1", "discount-1"));

    assertThat(response.getSubtotal()).isEqualByComparingTo("25.00");
    assertThat(response.getDiscountAmount()).isEqualByComparingTo("2.50");
    assertThat(response.getShippingFee()).isEqualByComparingTo("0.00");
    assertThat(response.getTotal()).isEqualByComparingTo("22.50");
}
```

Put these concrete fixture helpers in the same `OrderServiceTest` class:

```java
private CartItem item(String productVariantId, int quantity) {
    CartItem item = new CartItem();
    item.setProductVariantId(productVariantId);
    item.setProductQuantity(quantity);
    return item;
}

private void stubCart(String userId, CartItem... items) {
    Cart cart = new Cart();
    cart.setId("cart-1");
    cart.setUserId(userId);
    when(cartRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(cart));
    when(cartItemRepository.findAllByCartIdAndDeletedFalse("cart-1")).thenReturn(List.of(items));
}

private void stubVariant(String id, String name, String price) {
    ProductVariant variant = new ProductVariant();
    variant.setId(id);
    variant.setProductVariant(name);
    variant.setPrice(new BigDecimal(price));
    when(productVariantRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(variant));
}

private void stubDiscount(String id, String percentage) {
    Discount discount = new Discount();
    discount.setId(id);
    discount.setPercentageDiscount(new BigDecimal(percentage));
    when(discountRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(discount));
}
```

- [ ] **Step 2: Run the test to verify it fails.**

Run: `./mvnw -Dtest=OrderServiceTest#calculateSummaryAppliesDiscountToCurrentCartPrices test`

Expected: compilation fails because `OrderService` does not exist.

- [ ] **Step 3: Implement summary calculation and route.**

```java
public OrderSummaryResponse calculateSummary(OrderSummaryRequest request) {
    OrderContext context = loadOrderContext(request.userId(), request.discountId());
    return context.summary();
}

private BigDecimal money(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
}
```

`loadOrderContext` finds the active cart, rejects an empty cart with `BAD_REQUEST`, loads every active product variant, and optionally loads the active discount. It calculates subtotal from current prices, discount amount as `subtotal * percentageDiscount / 100`, shipping fee as `0.00`, and total as `subtotal - discountAmount`. `OrderController` adds:

```java
@PostMapping("/summary")
public BaseResponse<OrderSummaryResponse> calculateSummary(@Valid @RequestBody OrderSummaryRequest request) {
    return BaseResponse.success(orderService.calculateSummary(request));
}
```

The existing class-level `@RequestMapping("/order")` must be changed to `@RequestMapping("/api/orders")` so the final route is exactly `/api/orders/summary`.

- [ ] **Step 4: Run the focused test.**

Run: `./mvnw -Dtest=OrderServiceTest#calculateSummaryAppliesDiscountToCurrentCartPrices test`

Expected: `BUILD SUCCESS` and one passing test.

### Task 6: Create-order API

**Files:**
- Modify: `src/main/java/com/example/order_services/service/OrderService.java`
- Modify: `src/main/java/com/example/order_services/controller/OrderController.java`
- Test: `src/test/java/com/example/order_services/service/OrderServiceTest.java`

**Interfaces:**
- Consumes: `OrderService.calculateSummary` data, `CreateOrderRequest`, `Order`, `OrderItem`, `OrderStateRepository`, `OrderRepository`, `OrderItemRepository`, and `InventoryRepository`.
- Produces: `OrderService.createOrder(CreateOrderRequest request)` and `POST /api/orders`.

- [ ] **Step 1: Write the failing transactional-order test.**

```java
@Test
void createOrderSavesPriceSnapshotsReducesStockAndClearsCart() {
    CartItem cartItem = item("variant-a", 2);
    stubCart("user-1", cartItem);
    stubVariant("variant-a", "Tea", "10.00");
    Inventory inventory = stubInventory("variant-a", 3);
    stubPendingState();
    when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
        OrderEntity order = invocation.getArgument(0);
        order.setId("order-1");
        return order;
    });

    OrderResponse response = service.createOrder(new CreateOrderRequest("user-1", "address-1", "payment-1", null));

    assertThat(response.getTotal()).isEqualByComparingTo("20.00");
    assertThat(inventory.getQuantityInStock()).isEqualTo(1);
    assertThat(cartItem.isDeleted()).isTrue();
    verify(orderItemRepository).saveAll(anyList());
}
```

Add these remaining fixture helpers to `OrderServiceTest`:

```java
private Inventory stubInventory(String productVariantId, int quantity) {
    Inventory inventory = new Inventory();
    inventory.setProductVariantId(productVariantId);
    inventory.setQuantityInStock(quantity);
    when(inventoryRepository.findByProductVariantIdAndDeletedFalse(productVariantId))
        .thenReturn(Optional.of(inventory));
    return inventory;
}

private void stubPendingState() {
    OrderState pending = new OrderState();
    pending.setId("state-pending");
    pending.setState("PENDING");
    when(orderStateRepository.findByStateAndDeletedFalse("PENDING"))
        .thenReturn(Optional.of(pending));
}
```

- [ ] **Step 2: Run the test to verify it fails.**

Run: `./mvnw -Dtest=OrderServiceTest#createOrderSavesPriceSnapshotsReducesStockAndClearsCart test`

Expected: compilation fails because `createOrder` does not exist.

- [ ] **Step 3: Implement the transaction and route.**

```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest request) {
    OrderContext context = loadOrderContext(request.userId(), request.discountId());
    requireAvailableStock(context.items());
    OrderEntity order = orderRepository.save(newOrder(request, context.summary()));
    orderItemRepository.saveAll(newOrderItems(order.getId(), context.items()));
    reduceInventoryAndClearCart(context.items());
    return toOrderResponse(order, context.summary());
}

@PostMapping
public BaseResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return BaseResponse.success(orderService.createOrder(request));
}
```

`requireAvailableStock` loads each active inventory by variant ID and throws `BAD_REQUEST` if `quantityInStock < productQuantity`. `newOrder` assigns `PENDING` from `OrderStateRepository`, copies `discountId`, and copies all four calculated money values, including the denormalized `discountAmount`. `newOrderItems` copies the current unit price, cart quantity, and calculated line total. `reduceInventoryAndClearCart` subtracts quantities, saves inventories, sets each cart item's `deleted` field to `true`, and saves the cart items. The `@Transactional` annotation ensures an exception rolls back all five write steps.

- [ ] **Step 4: Run the focused test.**

Run: `./mvnw -Dtest=OrderServiceTest#createOrderSavesPriceSnapshotsReducesStockAndClearsCart test`

Expected: `BUILD SUCCESS` and one passing test.

### Task 7: Compile and run the complete verification suite

**Files:**
- Modify only if a compiler or test failure proves a mismatch in Tasks 1–6.
- Test: all files under `src/test/java/`.

**Interfaces:**
- Consumes: all implementation and tests from Tasks 1–6.
- Produces: a verified five-endpoint application.

- [ ] **Step 1: Compile production code.**

Run: `./mvnw -DskipTests compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run all unit tests.**

Run: `./mvnw test`

Expected: `BUILD SUCCESS`; no test needs a running MySQL instance.

- [ ] **Step 3: Inspect the routes and changed files.**

Run: `rg -n '@(Get|Post|Put|Delete|Patch)Mapping' src/main/java/com/example/order_services/controller && git diff --check`

Expected: exactly five route annotations for the requested APIs and no whitespace errors.

## Plan Self-Review

- Spec coverage: Tasks 1–2 create the requested base/error/model/repository/mapper foundation; Tasks 3–6 each cover the five approved endpoints; Task 6 persists the denormalized `discountAmount`; Task 7 verifies the whole result.
- Placeholder scan: no incomplete implementation instructions or deferred decisions remain.
- Type consistency: `OrderSummaryRequest`, `CreateOrderRequest`, `OrderSummaryResponse`, and `OrderResponse` use the same field names in controller, service, and tests. `Order` is the sole mapped order class, avoiding collision with framework types.
