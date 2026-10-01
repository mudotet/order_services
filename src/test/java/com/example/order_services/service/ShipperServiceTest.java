package com.example.order_services.service;

import com.example.order_services.common.DeliveryFailureReason;
import com.example.order_services.common.OrderStatus;
import com.example.order_services.dto.request.*;
import com.example.order_services.entity.*;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.impl.OrderServiceImpl;
import com.example.order_services.service.impl.ShipperServiceImpl;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipperServiceTest {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderStateRepository states = mock(OrderStateRepository.class);
    private final TrackingLogRepository logs = mock(TrackingLogRepository.class);
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final CurrentUserService currentUser = new CurrentUserService(users);
    private final OrderServiceImpl transitions = new OrderServiceImpl(mock(CartRepository.class),
            mock(CartItemRepository.class), mock(UserDiscountRepository.class), mock(InventoryRepository.class),
            states, orders, mock(OrderItemRepository.class), mock(OrderReturnRepository.class),
            mock(OrderReturnItemRepository.class), currentUser, mock(org.modelmapper.ModelMapper.class),
            users, notifications, logs, mock(UserRoleRepository.class));
    private final ShipperService shipperService = new ShipperServiceImpl(orders, addresses, currentUser, transitions);
    private Order order;
    private Address address;
    private User shipper;

    @BeforeEach
    void setUp() {
        shipper = User.builder().email("shipper@example.com").build();
        shipper.setId(UUID.randomUUID().toString());
        authenticate("SHIPPER");
        when(users.findByEmailAndDeletedFalse(shipper.getEmail())).thenReturn(Optional.of(shipper));
        User customer = User.builder().userName("Alice").email("alice@example.com").build();
        order = Order.builder().shipper(shipper).user(customer).addressId(UUID.randomUUID().toString())
                .deliveryAttemptId(UUID.randomUUID().toString()).orderState(state("PROCESSING")).build();
        order.setId(UUID.randomUUID().toString());
        address = Address.builder().address("123 Main Street").city("Hanoi").build();
        when(orders.findByIdAndDeletedFalse(order.getId())).thenReturn(Optional.of(order));
        when(addresses.findByIdAndDeletedFalse(order.getAddressId())).thenReturn(Optional.of(address));
        for (OrderStatus status : OrderStatus.values()) {
            when(states.findByStateAndDeletedFalse(status.name())).thenReturn(Optional.of(state(status.name())));
        }
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void receivesAssignedOrderAndDuplicateDoesNotAppendHistory() {
        ReceiveOrderRequest request = new ReceiveOrderRequest();
        request.setOrderId(order.getId());
        String token = order.getDeliveryAttemptId();
        assertThat(shipperService.receive(request).getDeliveryAttemptId()).isEqualTo(token);
        assertThat(shipperService.receive(request).getState()).isEqualTo("SHIPPING");
        verify(logs, times(1)).save(any());
        verify(notifications, times(1)).save(any());
    }

    @Test
    void rejectsOtherShipperAndUnassignedOrder() {
        ReceiveOrderRequest request = new ReceiveOrderRequest();
        request.setOrderId(order.getId());
        User other = new User();
        other.setId(UUID.randomUUID().toString());
        order.setShipper(other);
        assertThatThrownBy(() -> shipperService.receive(request)).isInstanceOf(ApplicationException.class);
        order.setShipper(null);
        assertThatThrownBy(() -> shipperService.receive(request)).isInstanceOf(ApplicationException.class);
        verifyNoInteractions(logs, notifications);
    }

    @Test
    void validatesIdentityAndTokenWithoutOverwritingDatabase() {
        order.setOrderState(state("SHIPPING"));
        DeliveredOrderRequest request = deliveredRequest();
        request.setAddress("Wrong address");
        assertThatThrownBy(() -> shipperService.delivered(request)).hasMessageContaining("does not match");
        request.setAddress(" 123 Main Street ");
        request.setDeliveryAttemptId(UUID.randomUUID().toString());
        assertThatThrownBy(() -> shipperService.delivered(request)).hasMessageContaining("attempt changed");
        request.setDeliveryAttemptId(order.getDeliveryAttemptId());
        assertThat(shipperService.delivered(request).getState()).isEqualTo("DELIVERED");
        shipperService.delivered(request);
        assertThat(address.getAddress()).isEqualTo("123 Main Street");
        assertThat(order.getUser().getUserName()).isEqualTo("Alice");
        verify(logs, times(1)).save(any());
    }

    @Test
    void failureRequiresNoteAndOnlyAdminRetryRotatesTokenPreservingHistory() {
        order.setOrderState(state("SHIPPING"));
        FailedOrderRequest request = new FailedOrderRequest();
        request.setOrderId(order.getId());
        request.setDeliveryAttemptId(order.getDeliveryAttemptId());
        request.setCustomerName("Alice");
        request.setAddress("123 Main Street");
        request.setFailureReason(DeliveryFailureReason.OTHER);
        request.setNote(" ");
        assertThatThrownBy(() -> shipperService.failed(request)).hasMessageContaining("OTHER");
        request.setNote("x".repeat(501));
        assertThatThrownBy(() -> shipperService.failed(request)).hasMessageContaining("500");
        request.setNote(" Customer requested another day ");
        shipperService.failed(request);
        String failedToken = order.getDeliveryAttemptId();
        ReceiveOrderRequest receive = new ReceiveOrderRequest();
        receive.setOrderId(order.getId());
        assertThatThrownBy(() -> shipperService.receive(receive)).isInstanceOf(ApplicationException.class);
        UpdateOrderStateRequest retry = UpdateOrderStateRequest.builder().state(OrderStatus.SHIPPING)
                .deliveryAttemptId(failedToken).build();
        assertThatThrownBy(() -> transitions.updateOrderState(order.getId(), retry)).hasMessageContaining("Only admins");
        authenticate("ADMIN");
        transitions.updateOrderState(order.getId(), retry);
        assertThat(order.getDeliveryAttemptId()).isNotEqualTo(failedToken);
        authenticate("SHIPPER");
        assertThatThrownBy(() -> shipperService.failed(request)).hasMessageContaining("attempt changed");
        verify(logs, times(2)).save(any());
        verify(logs, never()).delete(any());
    }

    @Test
    void cannotStartShippingWithoutAdminAssignment() {
        authenticate("ADMIN");
        order.setShipper(null);
        assertThatThrownBy(() -> transitions.updateOrderState(order.getId(),
                UpdateOrderStateRequest.builder().state(OrderStatus.SHIPPING).build()))
                .hasMessageContaining("assign a shipper");
    }

    @Test
    void requestConstraintsRejectMissingFieldsInvalidUuidAndOversizedNote() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new ReceiveOrderRequest())).isNotEmpty();
            FailedOrderRequest request = new FailedOrderRequest();
            request.setOrderId("product-code");
            request.setNote("x".repeat(501));
            assertThat(validator.validate(request)).hasSize(6);
        }
    }

    private DeliveredOrderRequest deliveredRequest() {
        DeliveredOrderRequest request = new DeliveredOrderRequest();
        request.setOrderId(order.getId());
        request.setDeliveryAttemptId(order.getDeliveryAttemptId());
        request.setCustomerName(" Alice ");
        request.setAddress("123 Main Street");
        return request;
    }

    private OrderState state(String name) {
        return OrderState.builder().state(name).build();
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                shipper.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
