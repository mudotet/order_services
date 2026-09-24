package com.example.order_services.entity;

import com.example.order_services.common.DiscountType;
import com.example.order_services.repository.UserDiscountRepository;
import com.example.order_services.repository.UserRoleRepository;
import com.example.order_services.repository.OrderRepository;
import com.example.order_services.repository.OrderItemRepository;
import org.hibernate.cfg.Configuration;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EntitySchemaTest {
    @Test
    void allEntitiesCanPersistAndReadTheSuppliedSchema() throws Exception {
        String url = "jdbc:h2:mem:entity-schema;MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var schema = new InputStreamReader(
                     getClass().getResourceAsStream("/entity-schema.sql"), StandardCharsets.UTF_8)) {
            RunScript.execute(connection, schema);
        }
        var configuration = new Configuration()
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.hbm2ddl.auto", "validate");
        String[] entities = {"User", "Address", "Cart", "Discount", "Notification", "OrderState",
                "Payment", "Order", "OrderReturn", "Permission", "Product", "ProductVariant",
                "CartItem", "Inventory", "OrderItem", "OrderReturnItem", "Role", "RolePermission",
                "TrackingLog", "UserDiscount", "UserRole"};
        for (String entity : entities) {
            configuration.addAnnotatedClass(Class.forName("com.example.order_services.entity." + entity));
        }
        try (var factory = configuration.buildSessionFactory(); var session = factory.openSession()) {
            var transaction = session.beginTransaction();
            User user = User.builder().userName("alice").email("alice@example.com").password("hash").build();
            session.persist(user);
            Role role = Role.builder().roleName("USER").build();
            session.persist(role);
            Permission permission = Permission.builder().permissionName("READ_ORDERS").build();
            session.persist(permission);
            var repositories = new JpaRepositoryFactory(session);
            var userRoles = repositories.getRepository(UserRoleRepository.class);
            var discounts = repositories.getRepository(UserDiscountRepository.class);
            userRoles.save(UserRole.builder().user(user).role(role).build());
            session.persist(RolePermission.builder().role(role).permission(permission).build());
            Product product = Product.builder().productName("Tea").build();
            product.setCreatedBy(user.getId());
            session.persist(product);
            ProductVariant variant = ProductVariant.builder().product(product).productVariant("Large")
                    .price(new BigDecimal("1234567890123.45")).build();
            session.persist(variant);
            session.persist(Inventory.builder().productVariant(variant).build());
            Cart cart = Cart.builder().user(user).build();
            session.persist(cart);
            session.persist(CartItem.builder().cart(cart).productVariant(variant).productQuantity(1).build());
            Address address = Address.builder().address("123 Test Street").city("Hà Nội").build();
            session.persist(address);
            Payment payment = Payment.builder().paymentMethod("CASH").build();
            session.persist(payment);
            Discount discount = Discount.builder().discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("10.25")).build();
            session.persist(discount);
            UserDiscount assignment = UserDiscount.builder().user(user).discount(discount).status("AVAILABLE").build();
            session.persist(assignment);
            OrderState state = OrderState.builder().state("PENDING").build();
            session.persist(state);
            Order order = Order.builder().user(user).discount(discount).addressId(address.getId())
                    .paymentId(payment.getId()).orderState(state).estimatedDelivery(LocalDate.of(2026, 9, 20)).build();
            session.persist(order);
            OrderItem item = OrderItem.builder().order(order).productVariant(variant).quantity(1)
                    .unitPrice(new BigDecimal("12.34")).lineTotal(new BigDecimal("12.34")).build();
            session.persist(item);
            OrderReturn orderReturn = OrderReturn.builder().returnCode("RETURN-001").order(order)
                    .status("REQUESTED").originType("CUSTOMER").build();
            session.persist(orderReturn);
            session.persist(OrderReturnItem.builder().orderReturn(orderReturn).orderItem(item).quantity(1)
                    .reasonType("DAMAGED").unitPrice(new BigDecimal("12.34")).refundAmount(new BigDecimal("12.34")).build());
            session.persist(new Notification());
            TrackingLog log = TrackingLog.builder().order(order).newStatus(state).location(address)
                    .createdBy(user.getId()).takeNote("Order received").build();
            session.persist(log);
            session.flush();
            session.clear();
            for (String entity : entities) {
                assertThat(session.createQuery("from " + entity, Object.class).getResultList()).hasSize(1);
            }
            assertThat(userRoles.findById(new UserRole.UserRoleId(user.getId(), role.getId()))).isPresent();
            assertThat(userRoles.findAllByUser_IdAndDeletedFalseAndRole_DeletedFalse(user.getId())).hasSize(1);
            assertThat(session.find(RolePermission.class,
                    new RolePermission.RolePermissionId(role.getId(), permission.getId()))).isNotNull();
            assertThat(session.find(ProductVariant.class, variant.getId()).getPrice())
                    .isEqualByComparingTo("1234567890123.45");
            assertThat(session.find(Order.class, order.getId()).getEstimatedDelivery()).isEqualTo("2026-09-20");
            assertThat(session.find(Order.class, order.getId()).getTotal()).isEqualByComparingTo("0.00");
            assertThat(session.find(TrackingLog.class, log.getId()).getLocation().getAddress()).isEqualTo("123 Test Street");
            assertThat(session.find(TrackingLog.class, log.getId()).getCreatedAt()).isNotNull();
            assertThat(session.find(Product.class, product.getId()).getCreatedBy()).isEqualTo(user.getId());
            assertThat(discounts.findAvailableByUserId(user.getId())).hasSize(1);
            UserDiscount available = discounts.findAvailableAssignment(user.getId(), discount.getId()).orElseThrow();
            LocalDateTime usedAt = LocalDateTime.of(2026, 9, 18, 10, 30);
            available.setUsedAt(usedAt);
            session.flush();
            session.clear();
            assertThat(session.find(UserDiscount.class, assignment.getId()).getUsedAt()).isEqualTo(usedAt);
            assertThat(discounts.findAvailableByUserId(user.getId())).isEmpty();
            assertThat(discounts.findAvailableAssignment(user.getId(), discount.getId())).isEmpty();
            var orders = repositories.getRepository(OrderRepository.class);
            var orderItems = repositories.getRepository(OrderItemRepository.class);
            var tracking = orders.findTrackingOrderInfo(order.getId(), user.getId()).orElseThrow();
            assertThat(tracking.getOrderTrackingId()).isEqualTo(order.getId());
            assertThat(tracking.getOrderTrackingStatus()).isEqualTo("PENDING");
            assertThat(tracking.getShippingAddress()).isEqualTo("123 Test Street");
            assertThat(tracking.getShippingCity()).isEqualTo("Hà Nội");
            assertThat(tracking.getEstimatedDelivery()).isEqualTo("2026-09-20");
            assertThat(tracking.getPaymentMethodInfo()).isEqualTo("CASH");
            assertThat(tracking.getTotalAmount()).isEqualByComparingTo("0.00");
            assertThat(orderItems.findPurchasedItems(order.getId(), user.getId())).singleElement().satisfies(purchased -> {
                assertThat(purchased.getPurchasedItemName()).isEqualTo("Tea");
                assertThat(purchased.getPurchasedItemDescription()).isEqualTo("Large");
                assertThat(purchased.getPurchasedItemQuantity()).isEqualTo(1);
                assertThat(purchased.getPurchasedItemPrice()).isEqualByComparingTo("12.34");
            });
            assertThat(orders.findTrackingOrderInfo(order.getId(), "another-user")).isEmpty();
            assertThat(orderItems.findPurchasedItems(order.getId(), "another-user")).isEmpty();
            assertThat(orders.findTrackingOrderInfo("missing-order", user.getId())).isEmpty();
            session.find(Order.class, order.getId()).setPaymentId(null);
            session.flush();
            assertThat(orders.findTrackingOrderInfo(order.getId(), user.getId()).orElseThrow().getPaymentMethodInfo()).isNull();
            session.find(OrderItem.class, item.getId()).setDeleted(true);
            session.flush();
            assertThat(orderItems.findPurchasedItems(order.getId(), user.getId())).isEmpty();
            session.find(Order.class, order.getId()).setDeleted(true);
            session.flush();
            assertThat(orders.findTrackingOrderInfo(order.getId(), user.getId())).isEmpty();
            transaction.commit();
        }
    }
}
