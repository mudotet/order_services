package com.example.order_services.service;

import com.example.order_services.common.DiscountType;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.entity.*;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final UserDiscountRepository discounts = mock(UserDiscountRepository.class);
    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final OrderStateRepository states = mock(OrderStateRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderItemRepository orderItems = mock(OrderItemRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderServiceImpl service = new OrderServiceImpl(carts, items, discounts, inventories, states,
            orders, orderItems, new CurrentUserService(users));
    private CartItem item;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
        User user = User.builder().userName("alice").build();
        user.setId("alice-id");
        when(users.findByUserNameAndDeletedFalse("alice")).thenReturn(Optional.of(user));
        Cart cart = Cart.builder().user(user).build();
        cart.setId("cart-id");
        when(carts.findByUser_IdAndDeletedFalse("alice-id")).thenReturn(Optional.of(cart));
        when(carts.findByUserIdForUpdate("alice-id")).thenReturn(Optional.of(cart));
        ProductVariant variant = ProductVariant.builder().price(new BigDecimal("12.50")).build();
        variant.setId("variant-id");
        item = CartItem.builder().cart(cart).productVariant(variant).productQuantity(2).build();
        when(items.findActiveItemsByCartId("cart-id")).thenReturn(List.of(item));
        inventory = Inventory.builder().productVariant(variant).quantityInStock(3).build();
        when(inventories.findByProductVariantIdsForUpdate(anyCollection())).thenReturn(List.of(inventory));
        when(states.findByStateAndDeletedFalse("PENDING")).thenReturn(Optional.of(OrderState.builder().state("PENDING").build()));
        when(orders.save(any())).thenAnswer(call -> {
            Order order = call.getArgument(0);
            order.setId("order-id");
            return order;
        });
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void summaryUsesAuthenticatedUsersCartBeforeAnyOrderExists() {
        assignDiscount(DiscountType.PERCENTAGE, "10");
        var result = service.calculateOrderSummary("discount-id");
        assertThat(result.getSubtotal()).isEqualByComparingTo("25.00");
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("2.50");
        assertThat(result.getShippingFee()).isEqualByComparingTo("30000.00");
        assertThat(result.getTotal()).isEqualByComparingTo("30022.50");
        verifyNoInteractions(orders, orderItems);
        verify(items, never()).saveAll(any());
        verify(discounts).findAvailableAssignment("alice-id", "discount-id");
    }

    @Test
    void percentageKeepsPrecisionUntilMoneyIsRounded() {
        assignDiscount(DiscountType.PERCENTAGE, "12.5");
        assertThat(service.calculateOrderSummary("discount-id").getDiscountAmount()).isEqualByComparingTo("3.13");
    }

    @Test
    void fixedDiscountCannotReduceShippingAndDiscountIsOptional() {
        assignDiscount(DiscountType.FIXED_AMOUNT, "30");
        assertThat(service.calculateOrderSummary("discount-id").getTotal()).isEqualByComparingTo("30000.00");
        assertThat(service.calculateOrderSummary(null).getTotal()).isEqualByComparingTo("30025.00");
    }

    @Test
    void rejectsUnavailableDiscountForPreviewAndCreation() {
        assertThatThrownBy(() -> service.calculateOrderSummary("unavailable-discount"))
                .isInstanceOf(ApplicationException.class).hasMessage("Discount unavailable");
        assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest("unavailable-discount", "address-id", "payment-id")))
                .isInstanceOf(ApplicationException.class).hasMessage("Discount unavailable");
        verifyNoInteractions(orders, orderItems);
    }

    @Test
    void createsOrderWithSameTotalsConsumesDiscountAndClearsOnlyItems() {
        var assignment = assignDiscount(DiscountType.PERCENTAGE, "10");
        var preview = service.calculateOrderSummary("discount-id");
        var response = service.createOrder(new CreateOrderRequest("discount-id", "address-id", "payment-id"));
        assertThat(response.getTotal()).isEqualByComparingTo(preview.getTotal());
        assertThat(response.getShippingFee()).isEqualByComparingTo("30000.00");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("2.50");
        assertThat(response.getState()).isEqualTo("PENDING");
        assertThat(inventory.getQuantityInStock()).isEqualTo(1);
        assertThat(item.isDeleted()).isTrue();
        assertThat(assignment.getUsed()).isTrue();
        verify(discounts, times(2)).findAvailableAssignment("alice-id", "discount-id");
        verify(orders).save(argThat(order -> order.getUser().getId().equals("alice-id")));
        verify(orderItems).saveAll(argThat(saved -> {
            OrderItem first = saved.iterator().next();
            return first.getQuantity() == 2 && first.getLineTotal().compareTo(new BigDecimal("25.00")) == 0;
        }));
        verify(carts, never()).deleteById(any());
    }

    @Test
    void insufficientStockDoesNotSaveOrder() {
        inventory.setQuantityInStock(1);
        assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest(null, "address-id", "payment-id")))
                .isInstanceOf(ApplicationException.class).hasMessage("Insufficient stock");
        verifyNoInteractions(orders, orderItems);
        assertThat(item.isDeleted()).isFalse();
    }

    @Test
    void canRemoveDiscountWhenCreatingAfterPreview() {
        var assignment = assignDiscount(DiscountType.PERCENTAGE, "10");
        assertThat(service.calculateOrderSummary("discount-id").getTotal()).isEqualByComparingTo("30022.50");
        var response = service.createOrder(new CreateOrderRequest(null, "address-id", "payment-id"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getTotal()).isEqualByComparingTo("30025.00");
        assertThat(assignment.getUsed()).isFalse();
        verify(orders).save(argThat(order -> order.getDiscount() == null));
        verify(discounts, never()).save(any());
    }

    @Test
    void duplicateVariantLinesCannotBypassStockCheck() {
        when(items.findActiveItemsByCartId("cart-id")).thenReturn(List.of(item, item));
        assertThatThrownBy(() -> service.createOrder(new CreateOrderRequest(null, "address-id", "payment-id")))
                .isInstanceOf(ApplicationException.class).hasMessage("Insufficient stock");
        verifyNoInteractions(orders, orderItems);
    }

    private UserDiscount assignDiscount(DiscountType type, String value) {
        Discount discount = Discount.builder().discountType(type).discountValue(new BigDecimal(value)).build();
        discount.setId("discount-id");
        UserDiscount assignment = UserDiscount.builder().discount(discount).used(false).status("AVAILABLE")
                .receivedAt(LocalDateTime.now().minusDays(1)).expiredAt(LocalDateTime.now().plusDays(1)).build();
        when(discounts.findAvailableAssignment("alice-id", "discount-id"))
                .thenReturn(Optional.of(assignment));
        return assignment;
    }
}
