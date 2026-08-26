package com.example.order_services.service;

import com.example.order_services.dto.response.CartDetailResponse;
import com.example.order_services.entity.Cart;
import com.example.order_services.entity.CartItem;
import com.example.order_services.entity.ProductVariant;
import com.example.order_services.repository.CartItemRepository;
import com.example.order_services.repository.CartRepository;
import com.example.order_services.repository.ProductVariantRepository;
import com.example.order_services.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void getCartReturnsAnEmptyCartWhenNoCartExists() {
        when(cartRepository.findByUserIdAndDeletedFalse("user-1"))
                .thenReturn(Optional.empty());

        CartDetailResponse response = cartService.getCart("user-1");

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void getCartCalculatesLineAndCartTotalsFromCurrentPrice() {
        Cart cart = new Cart();
        cart.setId("cart-1");
        cart.setUserId("user-1");
        CartItem item = new CartItem();
        item.setProductVariantId("variant-1");
        item.setProductQuantity(2);
        ProductVariant variant = new ProductVariant();
        variant.setId("variant-1");
        variant.setProductVariant("Large tea");
        variant.setPrice(new BigDecimal("7.50"));
        when(cartRepository.findByUserIdAndDeletedFalse("user-1"))
                .thenReturn(Optional.of(cart));
        when(cartItemRepository.findAllByCartIdAndDeletedFalse("cart-1"))
                .thenReturn(List.of(item));
        when(productVariantRepository.findByIdAndDeletedFalse("variant-1"))
                .thenReturn(Optional.of(variant));

        CartDetailResponse response = cartService.getCart("user-1");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getProductVariant()).isEqualTo("Large tea");
        assertThat(response.getItems().getFirst().getLineTotal())
                .isEqualByComparingTo("15.00");
        assertThat(response.getSubtotal()).isEqualByComparingTo("15.00");
    }
}
