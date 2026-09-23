package com.example.order_services;

import com.example.order_services.common.DiscountType;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.entity.*;
import com.example.order_services.entity.Order;
import com.example.order_services.repository.*;
import com.example.order_services.service.CurrentUserService;
import com.example.order_services.service.CartService;
import com.example.order_services.service.DiscountService;
import com.example.order_services.service.InventoryService;
import com.example.order_services.service.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:checkout-security;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false"
})
@Transactional
class CheckoutSecurityIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilterChain;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository users;
    @Autowired UserRoleRepository userRoles;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired InventoryRepository inventories;
    @Autowired DiscountRepository discounts;
    @Autowired UserDiscountRepository userDiscounts;
    @Autowired OrderStateRepository states;
    @Autowired OrderRepository orders;
    @Autowired CurrentUserService currentUserService;
    @Autowired CartService cartService;
    @Autowired DiscountService discountService;
    @Autowired InventoryService inventoryService;
    @Autowired OrderService orderService;
    @Autowired jakarta.persistence.EntityManager entityManager;

    MockMvc mvc;
    User alice;
    User bob;
    ProductVariant variant;
    Cart aliceCart;
    UserDiscount aliceDiscount;
    String bobDiscountId;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
        Role userRole = Role.builder().roleName("USER").build();
        entityManager.persist(userRole);
        Role adminRole = Role.builder().roleName("ADMIN").build();
        entityManager.persist(adminRole);
        alice = createUser("alice", userRole);
        bob = createUser("bob", userRole);
        createUser("admin", adminRole);
        Product product = products.save(Product.builder().productName("Tea").productType("Drink").build());
        variant = variants.save(ProductVariant.builder().product(product).productVariant("Large")
                .price(new BigDecimal("12.50")).build());
        inventories.save(Inventory.builder().productVariant(variant).quantityInStock(10).build());
        aliceCart = carts.save(Cart.builder().user(alice).build());
        items.save(CartItem.builder().cart(aliceCart).productVariant(variant).productQuantity(2).build());
        Cart bobCart = carts.save(Cart.builder().user(bob).build());
        items.save(CartItem.builder().cart(bobCart).productVariant(variant).productQuantity(5).build());
        aliceDiscount = assignDiscount(alice);
        bobDiscountId = assignDiscount(bob).getDiscount().getId();
        states.save(OrderState.builder().state("PENDING").build());
        entityManager.flush();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stateChangesPreserveSavedDeliveryDateAndTrackingCalculatesRemainingDays() throws Exception {
        states.save(OrderState.builder().state("PROCESSING").build());
        states.save(OrderState.builder().state("SHIPPING").build());
        states.save(OrderState.builder().state("DELIVERED").build());
        states.save(OrderState.builder().state("CANCELLED").build());
        for (String city : List.of("Hà Nội", "Cần Thơ")) {
            Address address = Address.builder().address("123 Test Street").city(city).build();
            entityManager.persist(address);
            Payment payment = Payment.builder().paymentMethod("CASH").build();
            entityManager.persist(payment);
            Order order = orders.save(Order.builder().user(alice).addressId(address.getId())
                    .paymentId(payment.getId())
                    .orderState(states.findByStateAndDeletedFalse("PENDING").orElseThrow()).build());
            order.setCreatedAt(LocalDateTime.now().minusDays(5));
            entityManager.flush();
            String trackingPath = "/api/orders/tracking/" + order.getId();
            String statePath = "/api/orders/tracking/" + order.getId() + "/state";
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            mvc.perform(get(trackingPath).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.estimatedDelivery").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.data.daysRemaining").value(org.hamcrest.Matchers.nullValue()));
            assertThat(orders.findById(order.getId()).orElseThrow().getEstimatedDelivery()).isNull();

            order.setEstimatedDelivery(today.plusDays(7));
            entityManager.flush();

            mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"state\":\"PENDING\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
            mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"state\":\"PROCESSING\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
            entityManager.flush();
            entityManager.clear();
            mvc.perform(get(trackingPath).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.*").value(org.hamcrest.Matchers.hasSize(9)))
                    .andExpect(jsonPath("$.data.orderTrackingId").value(order.getId()))
                    .andExpect(jsonPath("$.data.orderTrackingStatus").value("PROCESSING"))
                    .andExpect(jsonPath("$.data.purchasedItems").isEmpty())
                    .andExpect(jsonPath("$.data.totalAmount").value(0))
                    .andExpect(jsonPath("$.data.shippingAddress").value("123 Test Street"))
                    .andExpect(jsonPath("$.data.paymentMethodInfo").value("CASH"))
                    .andExpect(jsonPath("$.data.shippingCity").value(city))
                    .andExpect(jsonPath("$.data.estimatedDelivery").value(today.plusDays(7).toString()))
                    .andExpect(jsonPath("$.data.daysRemaining").value(7));

            orders.findById(order.getId()).orElseThrow().setEstimatedDelivery(today.plusDays(7));
            entityManager.flush();
            entityManager.clear();
            mvc.perform(get(trackingPath).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.estimatedDelivery").value(today.plusDays(7).toString()))
                    .andExpect(jsonPath("$.data.daysRemaining").value(7));

            orders.findById(order.getId()).orElseThrow().setEstimatedDelivery(today.minusDays(1));
            entityManager.flush();
            entityManager.clear();
            for (String next : List.of("PROCESSING", "SHIPPING", "DELIVERED")) {
                mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON)
                                .content("{\"state\":\"" + next + "\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
                entityManager.flush();
                entityManager.clear();
                mvc.perform(get(trackingPath).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.estimatedDelivery").value(today.minusDays(1).toString()))
                        .andExpect(jsonPath("$.data.daysRemaining").value(0));
            }
            Order cancelled = orders.save(Order.builder().user(alice).addressId(address.getId())
                    .paymentId(payment.getId()).estimatedDelivery(today.plusDays(7))
                    .orderState(states.findByStateAndDeletedFalse("PENDING").orElseThrow()).build());
            mvc.perform(authenticated(patch("/api/orders/tracking/" + cancelled.getId() + "/state"), "admin")
                            .contentType(MediaType.APPLICATION_JSON).content("{\"state\":\"CANCELLED\"}"))
                    .andExpect(status().isOk());
            entityManager.flush();
            entityManager.clear();
            assertThat(orders.findById(cancelled.getId()).orElseThrow().getEstimatedDelivery())
                    .isEqualTo(today.plusDays(7));
        }
    }

    @Test
    @DirtiesContext
    void stateUpdatesPersistWithoutCityAndRequireAdminAndForwardTransitions() throws Exception {
        states.save(OrderState.builder().state("PROCESSING").build());
        states.save(OrderState.builder().state("CANCELLED").build());
        Address address = Address.builder().address("123 Test Street").build();
        entityManager.persist(address);
        Payment payment = Payment.builder().paymentMethod("CASH").build();
        entityManager.persist(payment);
        Order order = orders.save(Order.builder().user(alice).addressId(address.getId())
                .paymentId(payment.getId())
                .orderState(states.findByStateAndDeletedFalse("PENDING").orElseThrow()).build());
        entityManager.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        String statePath = "/api/orders/tracking/" + order.getId() + "/state";
        mvc.perform(authenticated(patch(statePath), "alice").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"PROCESSING\"}"))
                .andExpect(status().isForbidden());
        for (String body : List.of("{}", "{\"state\":\"UNKNOWN\"}", "{\"state\":\"SHIPPING\"}")) {
            mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(order.getOrderState().getState()).isEqualTo("PENDING");
        assertThat(order.getEstimatedDelivery()).isNull();
        for (String next : List.of("PROCESSING", "CANCELLED")) {
            mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"state\":\"" + next + "\"}"))
                    .andExpect(status().isOk());
            Order savedOrder = orders.findById(order.getId()).orElseThrow();
            mvc.perform(get("/api/orders/tracking/" + order.getId())
                            .header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderTrackingStatus").value(next));
            assertThat(savedOrder.getEstimatedDelivery()).isNull();
            assertThat(savedOrder.getUpdatedBy()).isNotNull();
        }
        mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/orders/tracking/" + order.getId())
                        .header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estimatedDelivery").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.daysRemaining").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(authenticated(patch(statePath), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"PROCESSING\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(authenticated(patch("/api/orders/tracking/missing/state"), "admin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"PROCESSING\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void trackingReturnsPurchasedPricesAndRejectsOtherUsers() throws Exception {
        Address address = Address.builder().address("123 Test Street").build();
        entityManager.persist(address);
        Payment payment = Payment.builder().paymentMethod("CASH").build();
        entityManager.persist(payment);
        Order order = Order.builder().user(alice).addressId(address.getId()).paymentId(payment.getId())
                .orderState(states.findByStateAndDeletedFalse("PENDING").orElseThrow())
                .subtotal(new BigDecimal("19.50")).total(new BigDecimal("19.50")).build();
        entityManager.persist(order);
        entityManager.persist(OrderItem.builder().order(order).productVariant(variant).quantity(2)
                .unitPrice(new BigDecimal("9.75")).lineTotal(new BigDecimal("19.50")).build());
        entityManager.flush();
        String path = "/api/orders/tracking/" + order.getId();

        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderTrackingId").value(order.getId()))
                .andExpect(jsonPath("$.data.orderTrackingStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(19.50))
                .andExpect(jsonPath("$.data.shippingAddress").value("123 Test Street"))
                .andExpect(jsonPath("$.data.paymentMethodInfo").value("CASH"))
                .andExpect(jsonPath("$.data.purchasedItems.length()").value(1))
                .andExpect(jsonPath("$.data.purchasedItems[0].purchasedItemName").value("Tea"))
                .andExpect(jsonPath("$.data.purchasedItems[0].purchasedItemDescription").value("Large"))
                .andExpect(jsonPath("$.data.purchasedItems[0].purchasedItemQuantity").value(2))
                .andExpect(jsonPath("$.data.purchasedItems[0].purchasedItemPrice").value(9.75));
        mvc.perform(get(path).param("userId", alice.getId())
                        .header(HttpHeaders.AUTHORIZATION, basic("bob", "password")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Order not found"));
        mvc.perform(get("/api/orders/tracking/missing-order")
                        .header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Order not found"));
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, basic("admin", "password")))
                .andExpect(status().isForbidden());
        order.setDeleted(true);
        entityManager.flush();
        mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Order not found"));
    }

    @Test
    void exposesRequestedApis() {
        var mappings = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        var routes = mappings.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream().filter(path -> path.startsWith("/api/"))
                        .flatMap(path -> mapping.getMethodsCondition().getMethods().stream()
                                .map(method -> method + " " + path))).toList();
        assertThat(routes).containsExactlyInAnyOrder(
                "GET /api/carts", "GET /api/discounts", "POST /api/orders/summary", "POST /api/orders",
                "PUT /api/inventories/{productVariantId}/quantity", "PATCH /api/carts/items/{cartItemId}/quantity",
                "GET /api/orders/tracking/{id}", "GET /api/orders/returns",
                "GET /api/orders/returns/{id}", "GET /api/orders/returns/summary!", "PATCH /api/orders/tracking/{id}/state",
                "POST /api/auth/login");
    }

    @Test
    void cartButtonsChangeQuantityByOneAndRemoveItemAtZero() throws Exception {
        CartItem item = items.findActiveItemsByCartId(aliceCart.getId()).getFirst();
        String path = "/api/carts/items/" + item.getId() + "/quantity";
        mvc.perform(authenticated(patch(path), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityChange\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(3));
        mvc.perform(get("/api/carts").header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].cartItemId").value(item.getId()))
                .andExpect(jsonPath("$.data.subtotal").value(37.5));
        for (int expected : new int[]{2, 1, 0}) {
            mvc.perform(authenticated(patch(path), "alice").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantityChange\":-1}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(expected));
        }
        entityManager.flush();
        assertThat(item.isDeleted()).isTrue();
        assertThat(item.getProductQuantity()).isZero();
        assertThat(items.findActiveItemsByCartId(aliceCart.getId())).isEmpty();
        assertThat(inventories.findAll().getFirst().getQuantityInStock()).isEqualTo(10);
        mvc.perform(authenticated(patch(path), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityChange\":-1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cartQuantityRejectsInvalidStepsAndOtherUsersItems() throws Exception {
        CartItem ownItem = items.findActiveItemsByCartId(aliceCart.getId()).getFirst();
        CartItem foreignItem = items.findActiveItemsByCartId(
                carts.findByUser_IdAndDeletedFalse(bob.getId()).orElseThrow().getId()).getFirst();
        String path = "/api/carts/items/" + ownItem.getId() + "/quantity";
        for (String body : List.of("{}", "{\"quantityChange\":null}", "{\"quantityChange\":0}",
                "{\"quantityChange\":2}", "{\"quantityChange\":-2}")) {
            mvc.perform(authenticated(patch(path), "alice").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(authenticated(patch("/api/carts/items/" + foreignItem.getId() + "/quantity"), "alice")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":1,\"userId\":\"" + bob.getId() + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(authenticated(patch(path), "admin").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityChange\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch(path).header(HttpHeaders.AUTHORIZATION, basic("alice", "password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":1}"))
                .andExpect(status().isForbidden());
        assertThat(ownItem.getProductQuantity()).isEqualTo(2);
        assertThat(foreignItem.getProductQuantity()).isEqualTo(5);
    }

    @Test
    void cartIncreaseChecksCombinedStockAndDecreaseStillWorksAfterStockDrops() throws Exception {
        CartItem item = items.findActiveItemsByCartId(aliceCart.getId()).getFirst();
        String path = "/api/carts/items/" + item.getId() + "/quantity";
        items.save(CartItem.builder().cart(aliceCart).productVariant(variant).productQuantity(8).build());
        entityManager.flush();
        mvc.perform(authenticated(patch(path), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityChange\":1}"))
                .andExpect(status().isBadRequest());
        Inventory inventory = inventories.findAll().getFirst();
        inventory.setQuantityInStock(0);
        entityManager.flush();
        mvc.perform(authenticated(patch(path), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantityChange\":-1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));
        assertThat(item.getProductQuantity()).isEqualTo(1);
        assertThat(inventory.getQuantityInStock()).isZero();
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void simultaneousCartIncrementsDoNotLoseAnUpdate() throws Exception {
        String itemId = items.findActiveItemsByCartId(aliceCart.getId()).getFirst().getId();
        TestTransaction.flagForCommit();
        TestTransaction.end();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> increment = () -> {
                var request = authenticated(patch("/api/carts/items/" + itemId + "/quantity"), "alice")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantityChange\":1}");
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Increment did not start");
                return mvc.perform(request).andReturn().getResponse().getStatus();
            };
            var first = executor.submit(increment);
            var second = executor.submit(increment);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(items.findById(itemId).orElseThrow().getProductQuantity()).isEqualTo(4);
        assertThat(inventories.findAll().getFirst().getQuantityInStock()).isEqualTo(10);
    }

    @Test
    void authenticatesWithBasicAndReturnsOnlyCurrentUsersCart() throws Exception {
        mvc.perform(get("/api/carts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/carts").header(HttpHeaders.AUTHORIZATION, basic("alice", "wrong")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/carts").param("userId", bob.getId()).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.cartId").value(aliceCart.getId()))
                .andExpect(jsonPath("$.data.subtotal").value(25.00));
        mvc.perform(get("/api/carts/" + bob.getId()).header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void inventoryRequiresAdminEvenWhenServiceIsCalledDirectly() throws Exception {
        mvc.perform(authenticated(put("/api/inventories/" + variant.getId() + "/quantity"), "alice")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isForbidden());
        mvc.perform(authenticated(put("/api/inventories/" + variant.getId() + "/quantity"), "admin")
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.quantityInStock").value(12));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                alice.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertThatThrownBy(() -> inventoryService.updateQuantity(variant.getId(), new UpdateInventoryQuantityRequest(1)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void writesWithoutTokenStillRequireAuthenticationAndRole() throws Exception {
        String path = "/api/inventories/" + variant.getId() + "/quantity";
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, basic("alice", "password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(path).header(HttpHeaders.AUTHORIZATION, basic("admin", "password"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":12}"))
                .andExpect(status().isOk());
    }

    @Test
    void previewWorksBeforeOrderAndForeignDiscountIsRejected() throws Exception {
        assertThat(orders.count()).isZero();
        mvc.perform(authenticated(post("/api/orders/summary"), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"discountId\":\"" + aliceDiscount.getDiscount().getId() + "\",\"userId\":\"" + bob.getId() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.subtotal").value(25))
                .andExpect(jsonPath("$.data.discountAmount").value(2.5))
                .andExpect(jsonPath("$.data.shippingFee").value(30000)).andExpect(jsonPath("$.data.total").value(30022.5));
        assertThat(orders.count()).isZero();
        mvc.perform(authenticated(post("/api/orders/summary"), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"discountId\":\"" + bobDiscountId + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creationIgnoresInjectedUserIdAndSecondCheckoutCannotReuseCart() throws Exception {
        String body = "{\"userId\":\"" + bob.getId() + "\",\"discountId\":\"" + aliceDiscount.getDiscount().getId()
                + "\",\"addressId\":\"address-id\",\"paymentId\":\"payment-id\"}";
        mvc.perform(authenticated(post("/api/orders"), "alice").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.state").value("PENDING"))
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.total").value(30022.5));
        entityManager.flush();
        assertThat(orders.findAll()).singleElement().satisfies(order -> assertThat(order.getUser().getId()).isEqualTo(alice.getId()));
        assertThat(inventories.findAll().getFirst().getQuantityInStock()).isEqualTo(8);
        assertThat(items.findActiveItemsByCartId(aliceCart.getId())).isEmpty();
        assertThat(items.findActiveItemsByCartId(carts.findByUser_IdAndDeletedFalse(bob.getId()).orElseThrow().getId())).hasSize(1);
        assertThat(aliceDiscount.getUsedAt()).isNotNull();
        mvc.perform(authenticated(post("/api/orders"), "alice").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void creationUsesNewlySelectedDiscountAndIgnoresClientAmounts() throws Exception {
        UserDiscount newSelection = assignDiscount(alice);
        newSelection.getDiscount().setDiscountValue(new BigDecimal("20"));
        entityManager.flush();
        mvc.perform(authenticated(post("/api/orders/summary"), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"discountId\":\"" + aliceDiscount.getDiscount().getId() + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(30022.5));

        mvc.perform(authenticated(post("/api/orders"), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"discountId\":\"" + bobDiscountId + "\",\"addressId\":\"address-id\",\"paymentId\":\"payment-id\"}"))
                .andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();

        mvc.perform(authenticated(post("/api/orders"), "alice").contentType(MediaType.APPLICATION_JSON)
                .content("{\"discountId\":\"" + newSelection.getDiscount().getId()
                        + "\",\"value\":999,\"discountAmount\":999,\"shippingFee\":0,\"total\":0,\"addressId\":\"address-id\",\"paymentId\":\"payment-id\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.discountAmount").value(5))
                .andExpect(jsonPath("$.data.shippingFee").value(30000))
                .andExpect(jsonPath("$.data.total").value(30020));
        entityManager.flush();
        assertThat(orders.findAll()).singleElement().satisfies(order -> {
            assertThat(order.getDiscount().getId()).isEqualTo(newSelection.getDiscount().getId());
            assertThat(order.getDiscountAmount()).isEqualByComparingTo("5.00");
            assertThat(order.getShippingFee()).isEqualByComparingTo("30000.00");
            assertThat(order.getTotal()).isEqualByComparingTo("30020.00");
        });
        assertThat(aliceDiscount.getUsedAt()).isNull();
        assertThat(newSelection.getUsedAt()).isNotNull();
    }

    @Test
    void queriesExcludeDeletedUsersRolesProductsAndUnavailableDiscounts() throws Exception {
        UserDiscount expired = assignDiscount(alice);
        expired.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        UserDiscount used = assignDiscount(alice);
        used.setUsedAt(LocalDateTime.now());
        UserDiscount revoked = assignDiscount(alice);
        revoked.setStatus("REVOKED");
        UserDiscount deleted = assignDiscount(alice);
        deleted.getDiscount().setDeleted(true);
        UserDiscount future = assignDiscount(alice);
        future.setReceivedAt(LocalDateTime.now().plusHours(1));
        UserDiscount deletedAssignment = assignDiscount(alice);
        deletedAssignment.setDeleted(true);
        entityManager.flush();
        assertThat(userDiscounts.findAvailableByUserId(alice.getId())).hasSize(3);
        mvc.perform(get("/api/discounts").header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
        for (UserDiscount available : List.of(expired, future)) {
            mvc.perform(authenticated(post("/api/orders/summary"), "alice").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"discountId\":\"" + available.getDiscount().getId() + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.discountAmount").value(2.5));
        }
        for (UserDiscount unavailable : List.of(used, revoked, deleted, deletedAssignment)) {
            mvc.perform(authenticated(post("/api/orders/summary"), "alice").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"discountId\":\"" + unavailable.getDiscount().getId() + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        variant.getProduct().setDeleted(true);
        entityManager.flush();
        assertThat(items.findActiveItemsByCartId(aliceCart.getId())).isEmpty();
        var roles = userRoles.findAllByUser_IdAndDeletedFalseAndRole_DeletedFalse(alice.getId());
        roles.getFirst().setDeleted(true);
        entityManager.flush();
        mvc.perform(get("/api/carts").header(HttpHeaders.AUTHORIZATION, basic("alice", "password")))
                .andExpect(status().isForbidden());
        bob.setDeleted(true);
        entityManager.flush();
        mvc.perform(get("/api/carts").header(HttpHeaders.AUTHORIZATION, basic("bob", "password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void servicePermissionsSeparateCustomerAndAdminActions() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertThat(currentUserService.getCurrentUser().getUserName()).isEqualTo("admin");
        for (Runnable action : List.<Runnable>of(
                cartService::getCartDetail,
                () -> cartService.adjustCartItemQuantity("missing", null),
                discountService::getDiscounts,
                () -> orderService.calculateOrderSummary(null),
                () -> orderService.createOrder(null),
                () -> orderService.getTrackingOrderInfo("missing"))) {
            assertThatThrownBy(action::run).isInstanceOf(AccessDeniedException.class);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                alice.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertThat(currentUserService.getCurrentUser().getUserName()).isEqualTo("alice");
        for (Runnable action : List.<Runnable>of(
                () -> inventoryService.updateQuantity("missing", null),
                orderService::calculateOrderReturnSummary,
                () -> orderService.getOrderReturns(0, 4, "all requests"),
                () -> orderService.viewOrderReturnDetail("missing"))) {
            assertThatThrownBy(action::run).isInstanceOf(AccessDeniedException.class);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                bob.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        assertThat(orderService.calculateOrderReturnSummary()).isNotNull();
    }

    @Test
    void currentUserServiceRejectsAnonymousOrMissingAuthentication() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(currentUserService::getCurrentUser).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "alice", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThatThrownBy(currentUserService::getCurrentUser).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void concurrentCheckoutCreatesExactlyOneOrder() throws Exception {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> checkout = () -> {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        alice.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                ready.countDown();
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Checkout did not start");
                    orderService.createOrder(new CreateOrderRequest(null, "address-id", "payment-id"));
                    return true;
                } catch (ApplicationException expected) {
                    assertThat(expected.getMessage()).isEqualTo("Cart is empty");
                    return false;
                } finally {
                    SecurityContextHolder.clearContext();
                }
            };
            var first = executor.submit(checkout);
            var second = executor.submit(checkout);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(orders.count()).isEqualTo(1);
            assertThat(inventories.findAll().getFirst().getQuantityInStock()).isEqualTo(8);
        }
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void persistenceFailureRollsBackOrderStockCartAndDiscount() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                alice.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        assertThatThrownBy(() -> orderService.createOrder(new CreateOrderRequest(
                aliceDiscount.getDiscount().getId(), null, "payment-id"))).isInstanceOf(RuntimeException.class);
        assertThat(orders.count()).isZero();
        assertThat(inventories.findAll().getFirst().getQuantityInStock()).isEqualTo(10);
        assertThat(items.findActiveItemsByCartId(aliceCart.getId())).hasSize(1);
        assertThat(userDiscounts.findById(aliceDiscount.getId())
                .orElseThrow().getUsedAt()).isNull();
    }

    private User createUser(String username, Role role) {
        User user = users.save(User.builder().userName(username).email(username + "@example.com")
                .password(passwordEncoder.encode("password")).build());
        userRoles.save(UserRole.builder().user(user).role(role).build());
        return user;
    }

    private UserDiscount assignDiscount(User user) {
        Discount discount = discounts.save(Discount.builder().discountType(DiscountType.PERCENTAGE)
                .discountValue(new BigDecimal("10")).build());
        return userDiscounts.save(UserDiscount.builder().user(user).discount(discount).status("AVAILABLE")
                .receivedAt(LocalDateTime.now().minusDays(1)).expiredAt(LocalDateTime.now().plusDays(1)).build());
    }

    private MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, String username) {
        return request.header(HttpHeaders.AUTHORIZATION, basic(username, "password"));
    }

    private String basic(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + "@example.com:" + password).getBytes(StandardCharsets.UTF_8));
    }
}
