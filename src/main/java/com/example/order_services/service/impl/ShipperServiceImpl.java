package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.common.OrderStatus;
import com.example.order_services.dto.request.*;
import com.example.order_services.dto.response.OrderDeliveryResponse;
import com.example.order_services.entity.Address;
import com.example.order_services.entity.Order;
import com.example.order_services.entity.User;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.AddressRepository;
import com.example.order_services.repository.OrderRepository;
import com.example.order_services.service.CurrentUserService;
import com.example.order_services.service.OrderService;
import com.example.order_services.service.ShipperService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasRole('SHIPPER')")
public class ShipperServiceImpl implements ShipperService {
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CurrentUserService currentUserService;
    private final OrderService orderService;

    @Override
    public OrderDeliveryResponse receive(ReceiveOrderRequest request) {
        Order order = assignedOrder(request.getOrderId());
        if (!Set.of("PROCESSING", "SHIPPING").contains(order.getOrderState().getState())) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Only a PROCESSING order can be received");
        }
        return orderService.updateOrderState(order.getId(), UpdateOrderStateRequest.builder()
                .state(OrderStatus.SHIPPING).deliveryAttemptId(order.getDeliveryAttemptId()).build());
    }

    @Override
    public OrderDeliveryResponse delivered(DeliveredOrderRequest request) {
        Order order = validateOutcome(request, OrderStatus.DELIVERED);
        return orderService.updateOrderState(order.getId(), UpdateOrderStateRequest.builder()
                .state(OrderStatus.DELIVERED).deliveryAttemptId(request.getDeliveryAttemptId())
                .recipientName(request.getCustomerName().strip()).build());
    }

    @Override
    public OrderDeliveryResponse failed(FailedOrderRequest request) {
        Order order = validateOutcome(request, OrderStatus.DELIVERY_FAILED);
        return orderService.updateOrderState(order.getId(), UpdateOrderStateRequest.builder()
                .state(OrderStatus.DELIVERY_FAILED).deliveryAttemptId(request.getDeliveryAttemptId())
                .failureReason(request.getFailureReason()).note(request.getNote()).build());
    }

    private Order assignedOrder(String orderId) {
        User shipper = currentUserService.getCurrentUser();
        Order order = orderRepository.findByIdAndDeletedFalse(orderId)
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order not found"));
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            throw new ApplicationException(EnumCode.NOT_FOUND, "Order not found");
        }
        if (order.getDeliveryAttemptId() == null) {
            throw new ApplicationException(EnumCode.CONFLICT, "Admin must assign a delivery attempt first");
        }
        return order;
    }

    private Order validateOutcome(DeliveredOrderRequest request, OrderStatus next) {
        Order order = assignedOrder(request.getOrderId());
        if (!Objects.equals(order.getDeliveryAttemptId(), request.getDeliveryAttemptId())) {
            throw new ApplicationException(EnumCode.CONFLICT, "Delivery attempt changed; reload the order");
        }
        Address address = addressRepository.findByIdAndDeletedFalse(order.getAddressId())
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order address not found"));
        if (!Objects.equals(order.getUser().getUserName().strip(), request.getCustomerName().strip())
                || !Objects.equals(address.getAddress().strip(), request.getAddress().strip())) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Customer name or address does not match the order");
        }
        if (!Set.of("SHIPPING", next.name()).contains(order.getOrderState().getState())) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Only a SHIPPING order can record this outcome");
        }
        return order;
    }
}
