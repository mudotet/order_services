package com.example.order_services.service;

import com.example.order_services.entity.*;
import com.example.order_services.repository.*;
import com.example.order_services.service.impl.CartServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CartServiceTest {
    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CartServiceImpl service = new CartServiceImpl(carts, items, inventories, new CurrentUserService(users));

    @BeforeEach
    void signIn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
        User user = User.builder().userName("alice").build();
        user.setId("alice-id");
        when(users.findByUserNameAndDeletedFalse("alice")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsEmptyCartForCurrentUser() {
        var response = service.getCartDetail();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo("0.00");
        verify(carts).findByUser_IdAndDeletedFalse("alice-id");
    }

    @Test
    void returnsNamesTotalsAndStockUsingOneInventoryBatch() {
        var item = stubCart(2, 3);
        var response = service.getCartDetail();
        assertThat(response.getCartId()).isEqualTo("cart-id");
        assertThat(response.getItems().getFirst().getProductName()).isEqualTo("Tea Large");
        assertThat(response.getItems().getFirst().getLineTotal()).isEqualByComparingTo("15.00");
        assertThat(response.getSubtotal()).isEqualByComparingTo("15.00");
        verify(inventories, times(1)).findAllByProductVariantIdInAndDeletedFalse(List.of("variant-id"));
        assertThat(item.getProductQuantity()).isEqualTo(2);
    }

    private CartItem stubCart(int quantity, int stock) {
        Cart cart = new Cart();
        cart.setId("cart-id");
        when(carts.findByUser_IdAndDeletedFalse("alice-id")).thenReturn(Optional.of(cart));
        Product product = Product.builder().productName("Tea").build();
        ProductVariant variant = ProductVariant.builder().product(product).productVariant("Large")
                .price(new BigDecimal("7.50")).build();
        variant.setId("variant-id");
        CartItem item = CartItem.builder().cart(cart).productVariant(variant).productQuantity(quantity).build();
        when(items.findActiveItemsByCartId("cart-id")).thenReturn(List.of(item));
        when(inventories.findAllByProductVariantIdInAndDeletedFalse(List.of("variant-id")))
                .thenReturn(List.of(Inventory.builder().productVariant(variant).quantityInStock(stock).build()));
        return item;
    }
}
