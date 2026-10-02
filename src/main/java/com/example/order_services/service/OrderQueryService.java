package com.example.order_services.service;

import com.example.order_services.common.EnumCode;
import com.example.order_services.dto.response.OrderViewResponse;
import com.example.order_services.entity.Order;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final CurrentUserService currentUserService;

    public Page<OrderViewResponse> orders(int page, int size, String state, String query, String scope) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Page must be nonnegative and size between 1 and 100");
        }
        String userId = "USER".equals(scope) ? currentUserService.getCurrentUser().getId() : null;
        String shipperId = "SHIPPER".equals(scope) ? currentUserService.getCurrentUser().getId() : null;
        return orderRepository.browse(userId, shipperId, state == null ? "" : state.trim(),
                query == null ? "" : query.trim(), PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).map(this::view);
    }

    public OrderViewResponse detail(String id, boolean shipper) {
        Order order = orderRepository.findFirstByIdAndDeletedFalseAndUser_DeletedFalse(id)
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order not found"));
        if (shipper && (order.getShipper() == null || !order.getShipper().getId()
                .equals(currentUserService.getCurrentUser().getId()))) {
            throw new ApplicationException(EnumCode.NOT_FOUND, "Order not found");
        }
        return view(order);
    }

    private OrderViewResponse view(Order order) {
        String address = addressRepository.findByIdAndDeletedFalse(order.getAddressId())
                .map(a -> a.getAddress()).orElse(null);
        List<OrderViewResponse.Item> items = orderItemRepository
                .findAllByOrder_IdAndDeletedFalseOrderByCreatedAtAscIdAsc(order.getId()).stream()
                .map(i -> new OrderViewResponse.Item(i.getProductVariant().getProduct().getProductName(),
                        i.getQuantity(), i.getUnitPrice())).toList();
        return new OrderViewResponse(order.getId(), order.getOrderState().getState(), order.getTotal(),
                order.getCreatedAt(), order.getUser().getUserName(), order.getUser().getUserName(), address,
                order.getShipper() == null ? null : order.getShipper().getId(), order.getDeliveryAttemptId(), items);
    }

    public List<OrderViewResponse.Shipper> shippers() {
        return userRepository.findActiveShippers().stream()
                .map(u -> new OrderViewResponse.Shipper(u.getId(), u.getUserName(), u.getEmail())).toList();
    }

    public List<OrderViewResponse.Catalog> catalog() {
        return inventoryRepository.findCatalog().stream().map(i -> {
            var v = i.getProductVariant();
            return new OrderViewResponse.Catalog(v.getId(), v.getId(), v.getProduct().getProductName(),
                    v.getProductVariant(), v.getPrice(), i.getQuantityInStock());
        }).toList();
    }

    public List<OrderViewResponse.AddressOption> addresses() {
        return orderRepository.findUserAddresses(currentUserService.getCurrentUser().getId()).stream()
                .map(a -> new OrderViewResponse.AddressOption(a.getId(), a.getAddress(), a.getCity())).toList();
    }

    public List<OrderViewResponse.PaymentOption> payments() {
        return orderRepository.findUserPayments(currentUserService.getCurrentUser().getId()).stream()
                .map(p -> new OrderViewResponse.PaymentOption(p.getId(), p.getPaymentMethod())).toList();
    }
}
