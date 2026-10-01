package com.example.order_services.service;

import com.example.order_services.common.OrderStatus;
import com.example.order_services.dto.request.UpdateOrderStateRequest;
import com.example.order_services.entity.Order;
import com.example.order_services.entity.OrderState;
import com.example.order_services.entity.TrackingLog;
import com.example.order_services.entity.User;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.impl.OrderServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeliveryRecipientTest {
    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_SHIPPER"})
    void requiresNameAndKeepsDeliveredNameImmutable(String role) {
        User actor = User.builder().build();
        actor.setId("actor");
        User customer = User.builder().userName("Customer").email("customer@example.com").build();
        Order order = Order.builder().user(customer).shipper(actor).deliveryAttemptId("attempt")
                .orderState(OrderState.builder().state("SHIPPING").build()).build();
        order.setId("order");
        CurrentUserService currentUser = mock(CurrentUserService.class);
        OrderRepository orders = mock(OrderRepository.class);
        OrderStateRepository states = mock(OrderStateRepository.class);
        TrackingLogRepository logs = mock(TrackingLogRepository.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        AtomicReference<String> storedName = new AtomicReference<>();
        when(currentUser.getCurrentUser()).thenReturn(actor);
        when(orders.findByIdAndDeletedFalse("order")).thenReturn(Optional.of(order));
        when(states.findByStateAndDeletedFalse("DELIVERED"))
                .thenReturn(Optional.of(OrderState.builder().state("DELIVERED").build()));
        when(logs.findRecipientName("order")).thenAnswer(invocation -> Optional.ofNullable(storedName.get()));
        when(logs.save(any(TrackingLog.class))).thenAnswer(invocation -> {
            TrackingLog log = invocation.getArgument(0);
            storedName.set(log.getRecipientName());
            return log;
        });
        OrderServiceImpl service = new OrderServiceImpl(null, null, null, null, states, orders, null,
                null, null, currentUser, null, null, notifications, logs, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "actor", null, List.of(new SimpleGrantedAuthority(role))));
        try {
            for (String name : Arrays.asList(null, "", "   ", "x".repeat(256))) {
                UpdateOrderStateRequest request = UpdateOrderStateRequest.builder().state(OrderStatus.DELIVERED)
                        .deliveryAttemptId("attempt").recipientName(name).build();
                assertThatThrownBy(() -> service.updateOrderState("order", request))
                        .isInstanceOf(ApplicationException.class);
            }
            UpdateOrderStateRequest request = UpdateOrderStateRequest.builder().state(OrderStatus.SHIPPING)
                    .deliveryAttemptId("attempt").recipientName("Customer").build();
            assertThatThrownBy(() -> service.updateOrderState("order", request))
                    .isInstanceOf(ApplicationException.class);
            verify(logs, never()).save(any(TrackingLog.class));
            request.setState(OrderStatus.DELIVERED);
            request.setRecipientName("  Nguyễn Văn An  ");
            assertThat(service.updateOrderState("order", request).getRecipientName()).isEqualTo("Nguyễn Văn An");
            request.setRecipientName("Changed name");
            assertThat(service.updateOrderState("order", request).getRecipientName()).isEqualTo("Nguyễn Văn An");
            verify(logs, times(1)).save(any(TrackingLog.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
