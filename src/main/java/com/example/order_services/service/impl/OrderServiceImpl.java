package com.example.order_services.service.impl;

import com.example.order_services.common.DiscountType;
import com.example.order_services.common.EnumCode;
import com.example.order_services.common.OrderStatus;
import com.example.order_services.common.OrderReturnStatus;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.response.OrderResponse;
import com.example.order_services.dto.response.OrderReturnResponse;
import com.example.order_services.dto.response.OrderReturnsSummaryResponse;
import com.example.order_services.dto.response.OrderSummaryResponse;
import com.example.order_services.entity.*;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.CurrentUserService;
import com.example.order_services.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");


    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserDiscountRepository userDiscountRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderStateRepository orderStateRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final OrderReturnItemRepository orderReturnItemRepository;

    private final CurrentUserService currentUserService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;


    @Override
    public OrderReturnsSummaryResponse calculateOrderReturnSummary() {
        User user = currentUserService.getCurrentUser();
        if (!user.getUserName().equals("admin")){
            throw new ApplicationException(EnumCode.UNAUTHORIZED, "Unauthorized");
        }

        return OrderReturnsSummaryResponse.builder()
                .activeReturnChangePercentage(calActiveReturnChangePercentage())
                .activeReturnCount(calActiveReturnCount())
                .averageCycleTime(calAverageCycleTime())
                .awaitInspectionCount(calAwaitInspectionCount())
                .totalRefunds(calTotalRefunds())
                .build();
    }


    private BigDecimal calTotalRefunds() {
        return orderReturnRepository.getTotalRefundForSpecificQuarter(LocalDateTime.now().getYear(),
                (LocalDateTime.now().getMonthValue() - 1) / 3 + 1);
    }

    private Integer calAwaitInspectionCount() {
        return orderReturnRepository.getAwaitInspectionCount();
    }

    private Integer calAverageCycleTime() {
        List<OrderReturn> completedReturns = orderReturnRepository.getCompleteReturns();
        if (completedReturns.isEmpty()) {
            return 0;
        }
        long totalHours = completedReturns.stream()
                .mapToLong(orderReturn -> {
                    LocalDateTime requestedAt = orderReturn.getRequestedAt();
                    LocalDateTime refundedAt = orderReturn.getRefundedAt();
                    return java.time.Duration.between(requestedAt, refundedAt).toHours();
                })
                .sum();
        return (int) Math.round((double) totalHours / completedReturns.size());
    }

    private Integer calActiveReturnChangePercentage() {
        return 0;
    }

    private Integer calActiveReturnCount() {
        return orderReturnRepository.getActiveReturnCount();
    }

    @Override
    public Page<OrderReturnResponse> getOrderReturns(int page, int size) {
        User user = currentUserService.getCurrentUser();
        if(!user.getUserName().equals("admin")){
            throw new ApplicationException(EnumCode.UNAUTHORIZED, "Unauthorized");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        List<OrderReturn> orderReturnsPages = orderReturnRepository.findAll(pageable).getContent();
        Set<String> userIds = orderReturnsPages.stream()
                .map(orderReturn -> orderReturn.getOrder().getUser().getId())
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return orderReturnsPages.stream().map(orderReturn ->{
            User returnUser = userMap.get(orderReturn.getOrder().getUser().getId());
            LocalDateTime createdAt = orderReturn.getCreatedAt();

            OrderReturnResponse orderReturnResponse = new OrderReturnResponse();

            orderReturnResponse.setReturnId(orderReturn.getId());
            orderReturnResponse.setInitialTime(orderReturnResponse.calculateInitialTime(createdAt));
            orderReturnResponse.setCustomerName(returnUser.getUserName());
            orderReturnResponse.setReasonReturn(orderReturn.);
            orderReturnResponse.setOriginType(orderReturn.getOriginType());
            orderReturnResponse.setOrderReturnStatus(
                    orderReturn.getStatus().name()
            );
        });
    }

    @Override
    public OrderSummaryResponse calculateOrderSummary(String discountId) {
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUser_IdAndDeletedFalse(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty"));
        return loadCheckout(user.getId(), cart, discountId).summary();
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = currentUserService.getCurrentUser();
        // ponytail: write transaction deferred for learning; restore it before processing real orders.
        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty"));
        Checkout checkout = loadCheckout(user.getId(), cart, request.getDiscountId());
        Map<String, Long> quantities = new TreeMap<>();
        for (CartItem item : checkout.items()) {
            quantities.merge(item.getProductVariant().getId(), item.getProductQuantity().longValue(), Long::sum);
        }
        Map<String, Inventory> inventories = inventoryRepository.findByProductVariantIdsForUpdate(quantities.keySet())
                .stream().collect(Collectors.toMap(inventory -> inventory.getProductVariant().getId(), Function.identity()));
        for (var entry : quantities.entrySet()) {
            Inventory inventory = inventories.get(entry.getKey());
            if (inventory == null) {
                throw new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found");
            }
            if (inventory.getQuantityInStock() < entry.getValue()) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Insufficient stock");
            }
        }

        OrderState pending = orderStateRepository.findByStateAndDeletedFalse(OrderStatus.PENDING.name())
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "PENDING order state not found"));
        OrderSummaryResponse summary = checkout.summary();
        Order order = Order.builder()
                .user(user)
                .discount(checkout.assignment() == null ? null : checkout.assignment().getDiscount())
                .addressId(request.getAddressId())
                .paymentId(request.getPaymentId())
                .orderState(pending)
                .subtotal(summary.getSubtotal())
                .discountAmount(summary.getDiscountAmount())
                .shippingFee(summary.getShippingFee())
                .total(summary.getTotal())
                .build();
        orderRepository.save(order);
        List<OrderItem> orderItems = checkout.items().stream().map(item -> OrderItem.builder()
                .order(order)
                .productVariant(item.getProductVariant())
                .quantity(item.getProductQuantity())
                .unitPrice(item.getProductVariant().getPrice())
                .lineTotal(calculateLineTotal(item))
                .build()).toList();
        orderItemRepository.saveAll(orderItems);

        for (var entry : quantities.entrySet()) {
            Inventory inventory = inventories.get(entry.getKey());
            inventory.setQuantityInStock(inventory.getQuantityInStock() - entry.getValue().intValue());
        }
        inventoryRepository.saveAll(inventories.values());
        checkout.items().forEach(item -> item.setDeleted(true));
        cartItemRepository.saveAll(checkout.items());
        if (checkout.assignment() != null) {
            checkout.assignment().setUsed(true);
            checkout.assignment().setStatus("USED");
            userDiscountRepository.save(checkout.assignment());
        }
        return OrderResponse.builder()
                .id(order.getId())
                .state(pending.getState())
                .discountAmount(summary.getDiscountAmount())
                .subtotal(summary.getSubtotal())
                .shippingFee(summary.getShippingFee())
                .total(summary.getTotal())
                .build();
    }

    private Checkout loadCheckout(String userId, Cart cart, String discountId) {
        List<CartItem> items = cartItemRepository.findActiveItemsByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty");
        }
        BigDecimal subtotal = ZERO_MONEY;
        for (CartItem item : items) {
            if (item.getProductQuantity() == null || item.getProductQuantity() <= 0
                    || item.getProductVariant().getPrice() == null || item.getProductVariant().getPrice().signum() < 0) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid cart item");
            }
            subtotal = subtotal.add(calculateLineTotal(item));
        }
        UserDiscount assignment = null;
        BigDecimal discountAmount = ZERO_MONEY;
        if (discountId != null && !discountId.isBlank()) {
            assignment = userDiscountRepository.findAvailableAssignment(userId, discountId)
                    .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Discount unavailable"));
            Discount discount = assignment.getDiscount();
            BigDecimal discountValue = discount.getDiscountValue();
            if (discountValue == null || discountValue.signum() < 0 || discount.getDiscountType() == null
                    || (discount.getDiscountType() == DiscountType.PERCENTAGE && discountValue.compareTo(new BigDecimal("100")) > 0)) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid discount value");
            }
            if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = subtotal.multiply(discountValue)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                discountAmount = discountValue;
            }
            discountAmount = discountAmount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal shippingFee = new BigDecimal("30000.00");
        BigDecimal total = subtotal.subtract(discountAmount).add(shippingFee);
        OrderSummaryResponse summary = new OrderSummaryResponse(subtotal, discountAmount, shippingFee, total);
        return new Checkout(items, assignment, summary);
    }

    private BigDecimal calculateLineTotal(CartItem item) {
        return item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private record Checkout(List<CartItem> items, UserDiscount assignment, OrderSummaryResponse summary) {}
}
