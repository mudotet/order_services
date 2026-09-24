package com.example.order_services;

import com.example.order_services.entity.*;
import jakarta.persistence.EntityManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-returns;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false"
})
@Transactional
class OrderReturnsIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy securityFilterChain;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager entityManager;

    MockMvc mvc;
    OrderReturn pending;
    OrderReturn inTransit;
    OrderReturn deleted;
    OrderItem orderItem;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilterChain).build();
        createUser("manager", "ADMIN");
        User customer = createUser("admin", "USER");
        Address address = Address.builder().address("123 Test Street").build();
        entityManager.persist(address);
        OrderState state = OrderState.builder().state("DELIVERED").build();
        entityManager.persist(state);
        Order order = Order.builder().user(customer).addressId(address.getId()).orderState(state).build();
        entityManager.persist(order);
        Product product = Product.builder().productName("Tea").build();
        entityManager.persist(product);
        ProductVariant variant = ProductVariant.builder().product(product).productVariant("Large")
                .price(new BigDecimal("99.00")).build();
        entityManager.persist(variant);
        orderItem = OrderItem.builder().order(order).productVariant(variant).quantity(2)
                .unitPrice(new BigDecimal("12.50")).lineTotal(new BigDecimal("25.00")).build();
        entityManager.persist(orderItem);
        pending = createReturn(order, "RETURN-1", "PENDING", 30);
        inTransit = createReturn(order, "RETURN-2", "IN_TRANSIT", 60);
        deleted = createReturn(order, "RETURN-3", "PENDING", 10);
        deleted.setDeleted(true);
        entityManager.persist(OrderReturnItem.builder().orderReturn(pending).orderItem(orderItem)
                .quantity(1).reasonType("DAMAGED").conditionStatus("OPENED")
                .unitPrice(new BigDecimal("12.50")).refundAmount(new BigDecimal("12.50")).build());
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsActiveReturnsWithPaginationAndStatusFilters() throws Exception {
        mvc.perform(get("/api/orders/returns").param("size", "1").header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].returnId").value(pending.getId()))
                .andExpect(jsonPath("$.data.content[0].customerName").value("admin"))
                .andExpect(jsonPath("$.data.content[0].reasonReturn").value("DAMAGED"))
                .andExpect(jsonPath("$.data.content[0].initialTime", greaterThanOrEqualTo(30)));
        mvc.perform(get("/api/orders/returns").param("size", "1").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].returnId").value(inTransit.getId()));
        mvc.perform(get("/api/orders/returns").param("filterBy", "in-transit")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].orderReturnStatus").value("IN_TRANSIT"));
        mvc.perform(get("/api/orders/returns").param("page", "20")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void detailReturnsHeaderAndPurchasedItemValues() throws Exception {
        mvc.perform(get("/api/orders/returns/" + pending.getId()).header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.returnId").value(pending.getId()))
                .andExpect(jsonPath("$.data.orderReturnStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.originType").value("WEB"))
                .andExpect(jsonPath("$.data.customerName").value("admin"))
                .andExpect(jsonPath("$.data.reasonReturn").value("DAMAGED"))
                .andExpect(jsonPath("$.data.initialTime", greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].orderItemId").value(orderItem.getId()))
                .andExpect(jsonPath("$.data.items[0].productVariantId").value(orderItem.getProductVariant().getId()))
                .andExpect(jsonPath("$.data.items[0].productName").value("Tea"))
                .andExpect(jsonPath("$.data.items[0].reasonType").value("DAMAGED"))
                .andExpect(jsonPath("$.data.items[0].conditionStatus").value("OPENED"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andExpect(jsonPath("$.data.items[0].unitPrice").value(12.50))
                .andExpect(jsonPath("$.data.items[0].refundAmount").value(12.50));
        mvc.perform(get("/api/orders/returns/" + inTransit.getId()).header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        entityManager.createQuery("update OrderReturnItem set deleted = true").executeUpdate();
        entityManager.clear();
        mvc.perform(get("/api/orders/returns/" + pending.getId()).header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.reasonReturn").value(""));
    }

    @Test
    void protectsBothRoutesByRoleAndHandlesMissingAndDeletedReturns() throws Exception {
        for (String path : List.of("/api/orders/returns", "/api/orders/returns/" + pending.getId())) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, basic("admin")))
                    .andExpect(status().isForbidden());
        }
        for (String id : List.of("missing", deleted.getId())) {
            mvc.perform(get("/api/orders/returns/" + id).header(HttpHeaders.AUTHORIZATION, basic("manager")))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Test
    void rejectsInvalidPaginationAndFilters() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=101", "filterBy=unknown", "page=invalid")) {
            mvc.perform(get("/api/orders/returns?" + query).header(HttpHeaders.AUTHORIZATION, basic("manager")))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        }
    }

    @Test
    void exportsEveryCursorBatchOnceWithListColumnsAndFilters() throws Exception {
        Order order = entityManager.find(Order.class, orderItem.getOrder().getId());
        LocalDateTime sameTime = LocalDateTime.now().minusMinutes(90);
        for (int i = 0; i < 505; i++) {
            OrderReturn extra = OrderReturn.builder().order(order).returnCode("EXPORT-" + i)
                    .status("PENDING").originType("WEB").build();
            extra.setCreatedAt(sameTime);
            entityManager.persist(extra);
        }
        entityManager.flush();
        entityManager.clear();

        var response = mvc.perform(get("/api/orders/return/export").param("filterBy", "ALL_REQUESTS")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order-returns.csv"))
                .andReturn().getResponse();
        String csv = response.getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF");
        assertThat(response.getContentLengthLong()).isEqualTo(response.getContentAsByteArray().length);
        try (CSVParser parser = CSVParser.parse(csv.substring(1),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            assertThat(parser.getHeaderNames()).containsExactly(
                    "returnId", "initialTime", "customerName", "reasonReturn", "originType", "orderReturnStatus");
            List<CSVRecord> rows = parser.getRecords();
            assertThat(rows).hasSize(507);
            assertThat(rows.stream().map(row -> row.get("returnId")).toList())
                    .doesNotHaveDuplicates().doesNotContain(deleted.getId())
                    .contains(pending.getId(), inTransit.getId())
                    .isSortedAccordingTo(java.util.Comparator.reverseOrder());
            CSVRecord pendingRow = rows.stream().filter(row -> row.get("returnId").equals(pending.getId()))
                    .findFirst().orElseThrow();
            assertThat(pendingRow.get("reasonReturn")).isEqualTo("DAMAGED");
            assertThat(Long.parseLong(pendingRow.get("initialTime"))).isGreaterThanOrEqualTo(30);
        }
        String filtered = mvc.perform(get("/api/orders/return/export").param("filterBy", "in-transit")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(filtered).contains(inTransit.getId()).doesNotContain(pending.getId());
        String empty = mvc.perform(get("/api/orders/return/export").param("filterBy", "REFUNDED")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(empty.lines().count()).isEqualTo(1);
    }

    @Test
    void exportsOnlyRowsMatchingEachSupportedFilter() throws Exception {
        Order order = entityManager.find(Order.class, orderItem.getOrder().getId());
        for (String state : List.of("WAREHOUSE_RECEIVED", "RESTOCKED", "REFUNDED")) {
            createReturn(order, "EXPORT-" + state, state, 90);
        }
        entityManager.flush();
        entityManager.clear();

        for (String filter : List.of("ALL_REQUESTS", "PENDING", "IN_TRANSIT", "WAREHOUSE_RECEIVED", "RESTOCKED", "REFUNDED")) {
            String csv = mvc.perform(get("/api/orders/return/export").param("filterBy", filter)
                            .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            try (CSVParser parser = CSVParser.parse(csv.substring(1),
                    CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
                List<CSVRecord> rows = parser.getRecords();
                assertThat(rows).hasSize(filter.equals("ALL_REQUESTS") ? 5 : 1);
                if (!filter.equals("ALL_REQUESTS")) {
                    assertThat(rows.getFirst().get("orderReturnStatus")).isEqualTo(filter);
                }
            }
        }
    }

    @Test
    void exportPreservesCsvTextExcludesDeletedReasonsAndChecksAccess() throws Exception {
        String customerName = "=SUM(1,2)\nKhách \"An\"";
        User customer = entityManager.find(User.class, orderItem.getOrder().getUser().getId());
        customer.setUserName(customerName);
        entityManager.createQuery("update OrderReturnItem set deleted = true").executeUpdate();
        entityManager.flush();
        entityManager.clear();
        String csv = mvc.perform(get("/api/orders/return/export")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        try (CSVParser parser = CSVParser.parse(csv.substring(1),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            List<CSVRecord> rows = parser.getRecords();
            assertThat(rows).hasSize(2);
            assertThat(rows.getFirst().get("customerName")).isEqualTo(customerName);
            assertThat(rows.getFirst().get("reasonReturn")).isEmpty();
        }
        mvc.perform(get("/api/orders/return/export")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/return/export").header(HttpHeaders.AUTHORIZATION, basic("admin")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/orders/return/export").param("filterBy", "unknown")
                        .header(HttpHeaders.AUTHORIZATION, basic("manager")))
                .andExpect(status().isBadRequest());
    }

    private User createUser(String username, String roleName) {
        Role role = Role.builder().roleName(roleName).build();
        entityManager.persist(role);
        User user = User.builder().userName(username).email(username + "@example.com")
                .password(passwords.encode("password")).build();
        entityManager.persist(user);
        entityManager.persist(UserRole.builder().user(user).role(role).build());
        return user;
    }

    private OrderReturn createReturn(Order order, String code, String status, int minutesAgo) {
        OrderReturn orderReturn = OrderReturn.builder().order(order).returnCode(code).status(status).originType("WEB").build();
        orderReturn.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        entityManager.persist(orderReturn);
        return orderReturn;
    }

    private String basic(String username) {
        return "Basic " + Base64.getEncoder().encodeToString((username + "@example.com:password").getBytes(StandardCharsets.UTF_8));
    }
}
