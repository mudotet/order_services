package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.common.OrderStatus;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.OrderSummaryRequest;
import com.example.order_services.dto.response.OrderResponse;
import com.example.order_services.dto.response.OrderSummaryResponse;
import com.example.order_services.entity.Cart;
import com.example.order_services.entity.CartItem;
import com.example.order_services.entity.Discount;
import com.example.order_services.entity.Inventory;
import com.example.order_services.entity.OrderEntity;
import com.example.order_services.entity.OrderItem;
import com.example.order_services.entity.OrderState;
import com.example.order_services.entity.ProductVariant;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.CartItemRepository;
import com.example.order_services.repository.CartRepository;
import com.example.order_services.repository.DiscountRepository;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.repository.OrderItemRepository;
import com.example.order_services.repository.OrderRepository;
import com.example.order_services.repository.OrderStateRepository;
import com.example.order_services.repository.ProductVariantRepository;
import com.example.order_services.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final DiscountRepository discountRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderStateRepository orderStateRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public OrderSummaryResponse calculateSummary(OrderSummaryRequest request) {
        return loadOrderContext(request.getUserId(), request.getDiscountId()).summary();
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Cart cart = cartRepository.findForUpdateByUserId(request.getUserId())
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.BAD_REQUEST,
                        "Cart is empty"
                ));
        OrderContext context = loadOrderContext(cart, request.getDiscountId());
        List<Inventory> inventories = requireAvailableStock(context.lines());
        OrderState pending = orderStateRepository
                .findByStateAndDeletedFalse(OrderStatus.PENDING.name())
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.NOT_FOUND,
                        "PENDING order state not found"
                ));
        OrderEntity order = orderRepository.save(newOrder(request, pending, context.summary()));
        orderItemRepository.saveAll(newOrderItems(order.getId(), context.lines()));
        reduceInventoryAndClearCart(inventories, context.lines());
        return new OrderResponse(
                order.getId(),
                pending.getState(),
                context.summary().getSubtotal(),
                context.summary().getDiscountAmount(),
                context.summary().getShippingFee(),
                context.summary().getTotal()
        );
    }

    private OrderContext loadOrderContext(String userId, String discountId) {
        Cart cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.BAD_REQUEST,
                        "Cart is empty"
                ));
        return loadOrderContext(cart, discountId);
    }

    private OrderContext loadOrderContext(Cart cart, String discountId) {
        List<CartItem> cartItems = cartItemRepository
                .findAllByCartIdAndDeletedFalse(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty");
        }

        List<OrderLine> lines = cartItems.stream()
                .map(this::toOrderLine)
                .sorted(Comparator.comparing(line -> line.productVariant().getId()))
                .toList();
        BigDecimal subtotal = money(lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(ZERO_MONEY, BigDecimal::add));
        BigDecimal discountPercentage = findDiscountPercentage(discountId);
        BigDecimal discountAmount = money(
                subtotal.multiply(discountPercentage).divide(ONE_HUNDRED)
        );
        BigDecimal total = money(subtotal.subtract(discountAmount));
        return new OrderContext(
                lines,
                new OrderSummaryResponse(subtotal, discountAmount, ZERO_MONEY, total)
        );
    }

    private OrderLine toOrderLine(CartItem item) {
        ProductVariant variant = productVariantRepository
                .findByIdAndDeletedFalse(item.getProductVariantId())
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.NOT_FOUND,
                        "Product variant not found"
                ));
        BigDecimal lineTotal = money(
                variant.getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity()))
        );
        return new OrderLine(item, variant, lineTotal);
    }

    private BigDecimal findDiscountPercentage(String discountId) {
        if (discountId == null || discountId.isBlank()) {
            return BigDecimal.ZERO;
        }
        Discount discount = discountRepository.findByIdAndDeletedFalse(discountId)
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.NOT_FOUND,
                        "Discount not found"
                ));
        return discount.getPercentageDiscount();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Inventory> requireAvailableStock(List<OrderLine> lines) {
        return lines.stream().map(line -> {
            Inventory inventory = inventoryRepository
                    .findForUpdateByProductVariantId(line.productVariant().getId())
                    .orElseThrow(() -> new ApplicationException(
                            EnumCode.NOT_FOUND,
                            "Inventory not found"
                    ));
            if (inventory.getQuantityInStock() < line.cartItem().getProductQuantity()) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Insufficient stock");
            }
            return inventory;
        }).toList();
    }

    private OrderEntity newOrder(
            CreateOrderRequest request,
            OrderState pending,
            OrderSummaryResponse summary
    ) {
        OrderEntity order = new OrderEntity();
        order.setUserId(request.getUserId());
        order.setAddressId(request.getAddressId());
        order.setPaymentId(request.getPaymentId());
        order.setDiscountId(blankToNull(request.getDiscountId()));
        order.setOrderStateId(pending.getId());
        order.setSubtotal(summary.getSubtotal());
        order.setDiscountAmount(summary.getDiscountAmount());
        order.setShippingFee(summary.getShippingFee());
        order.setTotal(summary.getTotal());
        return order;
    }

    private List<OrderItem> newOrderItems(String orderId, List<OrderLine> lines) {
        return lines.stream().map(line -> {
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductVariantId(line.productVariant().getId());
            item.setUnitPrice(line.productVariant().getPrice());
            item.setQuantity(line.cartItem().getProductQuantity());
            item.setLineTotal(line.lineTotal());
            return item;
        }).toList();
    }

    private void reduceInventoryAndClearCart(
            List<Inventory> inventories,
            List<OrderLine> lines
    ) {
        for (int index = 0; index < lines.size(); index++) {
            Inventory inventory = inventories.get(index);
            CartItem cartItem = lines.get(index).cartItem();
            inventory.setQuantityInStock(
                    inventory.getQuantityInStock() - cartItem.getProductQuantity()
            );
            cartItem.setDeleted(true);
        }
        inventoryRepository.saveAll(inventories);
        cartItemRepository.saveAll(lines.stream().map(OrderLine::cartItem).toList());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record OrderLine(
            CartItem cartItem,
            ProductVariant productVariant,
            BigDecimal lineTotal
    ) {
    }

    private record OrderContext(
            List<OrderLine> lines,
            OrderSummaryResponse summary
    ) {
    }
}
