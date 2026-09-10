package com.example.order_services.service;

import com.example.order_services.common.EnumCode;
import com.example.order_services.entity.Order;
import com.example.order_services.entity.OrderReturn;
import com.example.order_services.entity.User;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.OrderReturnItemRepository;
import com.example.order_services.repository.OrderReturnRepository;
import com.example.order_services.repository.UserRepository;
import com.example.order_services.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEFAULTS;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderReturnFilterTest {
    private final List<OrderReturn> storedReturns = new ArrayList<>();
    private final OrderReturnRepository orderReturns = mock(OrderReturnRepository.class, this::answerReturnQuery);
    private final OrderReturnItemRepository returnItems = mock(OrderReturnItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(
            null, null, null, null, null, null, null,
            orderReturns, returnItems, new CurrentUserService(users), null, users
    );

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        User admin = User.builder().userName("admin").build();
        admin.setId("admin-id");
        User customer = User.builder().userName("customer").build();
        customer.setId("customer-id");
        when(users.findByUserNameAndDeletedFalse("admin")).thenReturn(Optional.of(admin));
        when(users.findAllById(anySet())).thenReturn(List.of(customer));

        storedReturns.clear();
        storedReturns.add(returnWithStatus("pending", "PENDING", customer, false));
        storedReturns.add(returnWithStatus("transit", "IN_TRANSIT", customer, false));
        storedReturns.add(returnWithStatus("received", "WAREHOUSE_RECEIVED", customer, false));
        storedReturns.add(returnWithStatus("restocked", "RESTOCKED", customer, false));
        storedReturns.add(returnWithStatus("refunded", "REFUNDED", customer, false));
        storedReturns.add(returnWithStatus("deleted", "PENDING", customer, true));
        when(returnItems.findByOrderReturnId(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @CsvSource({
            "Pending, PENDING",
            "in-transit, IN_TRANSIT",
            "Warehouse Received, WAREHOUSE_RECEIVED",
            "warehouse_received, WAREHOUSE_RECEIVED",
            "Restocked, RESTOCKED",
            "REFUNDED, REFUNDED"
    })
    void filtersBySupportedStatusLabels(String filterBy, String expectedStatus) {
        Page<?> result = service.getOrderReturns(0, 20, filterBy);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
                .extracting("orderReturnStatus")
                .containsExactly(expectedStatus);
    }

    @Test
    void allRequestsReturnsOnlyNonDeletedRows() {
        Page<?> result = service.getOrderReturns(0, 20, "all requests");

        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent())
                .extracting("returnId")
                .doesNotContain("deleted");
    }

    @Test
    void rejectsUnsupportedFilters() {
        for (String filterBy : Arrays.asList(null, "", "Inspecting", "unknown")) {
            assertThatThrownBy(() -> service.getOrderReturns(0, 20, filterBy))
                    .isInstanceOf(ApplicationException.class)
                    .extracting("code")
                    .isEqualTo(EnumCode.BAD_REQUEST);
        }
    }

    private Object answerReturnQuery(org.mockito.invocation.InvocationOnMock invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if (!Page.class.isAssignableFrom(invocation.getMethod().getReturnType())) {
            return RETURNS_DEFAULTS.answer(invocation);
        }

        Pageable pageable = Arrays.stream(invocation.getArguments())
                .filter(Pageable.class::isInstance)
                .map(Pageable.class::cast)
                .findFirst()
                .orElseThrow();
        List<OrderReturn> matches;
        if (methodName.equals("findAllByDeletedFalse")) {
            matches = storedReturns.stream().filter(orderReturn -> !orderReturn.isDeleted()).toList();
        } else if (methodName.equals("findAllByStatusAndDeletedFalse")) {
            String status = (String) invocation.getArguments()[0];
            matches = storedReturns.stream()
                    .filter(orderReturn -> !orderReturn.isDeleted() && status.equals(orderReturn.getStatus()))
                    .toList();
        } else {
            matches = List.copyOf(storedReturns);
        }
        return new PageImpl<>(matches, pageable, matches.size());
    }

    private OrderReturn returnWithStatus(String id, String status, User customer, boolean deleted) {
        OrderReturn orderReturn = OrderReturn.builder()
                .order(Order.builder().user(customer).build())
                .status(status)
                .originType("WEB")
                .build();
        orderReturn.setId(id);
        orderReturn.setDeleted(deleted);
        return orderReturn;
    }
}
