package com.example.order_services.service;

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
import com.example.order_services.repository.CartItemRepository;
import com.example.order_services.repository.CartRepository;
import com.example.order_services.repository.DiscountRepository;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.repository.OrderItemRepository;
import com.example.order_services.repository.OrderRepository;
import com.example.order_services.repository.OrderStateRepository;
import com.example.order_services.repository.ProductVariantRepository;
import com.example.order_services.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private OrderStateRepository orderStateRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void calculateSummaryAppliesDiscountToCurrentCartPrices() {
        stubCart("user-1", item("variant-a", 2), item("variant-b", 1));
        stubVariant("variant-a", "Tea", "10.00");
        stubVariant("variant-b", "Cake", "5.00");
        stubDiscount("discount-1", "10.00");

        OrderSummaryResponse response = orderService.calculateSummary(
                new OrderSummaryRequest("user-1", "discount-1")
        );

        assertThat(response.getSubtotal()).isEqualByComparingTo("25.00");
        assertThat(response.getDiscountAmount()).isEqualByComparingTo("2.50");
        assertThat(response.getShippingFee()).isEqualByComparingTo("0.00");
        assertThat(response.getTotal()).isEqualByComparingTo("22.50");
    }

    @Test
    void createOrderPersistsDiscountReducesStockAndClearsCart() {
        CartItem cartItem = item("variant-a", 2);
        stubLockedCart("user-1", cartItem);
        stubVariant("variant-a", "Tea", "10.00");
        stubDiscount("discount-1", "10.00");
        Inventory inventory = stubLockedInventory("variant-a", 3);
        stubPendingState();
        List<OrderItem> savedOrderItems = new ArrayList<>();
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(OrderEntity.class)))
                .thenAnswer(invocation -> {
                    OrderEntity order = invocation.getArgument(0);
                    order.setId("order-1");
                    return order;
                });
        when(orderItemRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> {
                    List<OrderItem> items = invocation.getArgument(0);
                    savedOrderItems.addAll(items);
                    return items;
                });

        OrderResponse response = orderService.createOrder(new CreateOrderRequest(
                "user-1",
                "address-1",
                "payment-1",
                "discount-1"
        ));

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getDiscountAmount()).isEqualByComparingTo("2.00");
        assertThat(savedOrderItems.getFirst().getUnitPrice())
                .isEqualByComparingTo("10.00");
        assertThat(response.getTotal()).isEqualByComparingTo("18.00");
        assertThat(inventory.getQuantityInStock()).isEqualTo(1);
        assertThat(cartItem.isDeleted()).isTrue();
    }

    private CartItem item(String productVariantId, int quantity) {
        CartItem item = new CartItem();
        item.setProductVariantId(productVariantId);
        item.setProductQuantity(quantity);
        return item;
    }

    private void stubCart(String userId, CartItem... items) {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId(userId);
        when(cartRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartIdAndDeletedFalse("cart-1"))
                .thenReturn(List.of(items));
    }

    private void stubLockedCart(String userId, CartItem... items) {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId(userId);
        when(cartRepository.findForUpdateByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartIdAndDeletedFalse("cart-1"))
                .thenReturn(List.of(items));
    }

    private void stubVariant(String id, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setProductVariant(name);
        variant.setPrice(new BigDecimal(price));
        when(productVariantRepository.findByIdAndDeletedFalse(id))
                .thenReturn(Optional.of(variant));
    }

    private void stubDiscount(String id, String percentage) {
        Discount discount = new Discount();
        discount.setId(id);
        discount.setPercentageDiscount(new BigDecimal(percentage));
        when(discountRepository.findByIdAndDeletedFalse(id))
                .thenReturn(Optional.of(discount));
    }

    private Inventory stubLockedInventory(String productVariantId, int quantity) {
        Inventory inventory = new Inventory();
        inventory.setProductVariantId(productVariantId);
        inventory.setQuantityInStock(quantity);
        when(inventoryRepository.findForUpdateByProductVariantId(productVariantId))
                .thenReturn(Optional.of(inventory));
        return inventory;
    }

    private void stubPendingState() {
        OrderState pending = new OrderState();
        pending.setId("state-pending");
        pending.setState("PENDING");
        when(orderStateRepository.findByStateAndDeletedFalse("PENDING"))
                .thenReturn(Optional.of(pending));
    }
}
