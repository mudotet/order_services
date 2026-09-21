package com.example.order_services.service;

import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.entity.Inventory;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final InventoryServiceImpl service = new InventoryServiceImpl(inventories);

    @Test
    void setsExactInventoryQuantityUsingLockedRow() {
        Inventory inventory = Inventory.builder().quantityInStock(3).build();
        inventory.setId("inventory-id");
        when(inventories.findByProductVariantIdsForUpdate(List.of("variant-id"))).thenReturn(List.of(inventory));
        var response = service.updateQuantity("variant-id", new UpdateInventoryQuantityRequest(12));
        assertThat(response.getQuantityInStock()).isEqualTo(12);
        assertThat(response.getProductVariantId()).isEqualTo("variant-id");
        assertThat(inventory.getQuantityInStock()).isEqualTo(12);
    }

    @Test
    void rejectsNegativeInventoryWithoutWriting() {
        assertThatThrownBy(() -> service.updateQuantity("variant-id", new UpdateInventoryQuantityRequest(-1)))
                .isInstanceOf(ApplicationException.class);
        verifyNoInteractions(inventories);
    }

    @Test
    void updatesProductVariantAndStockWithoutChangingIds() {
        var product = com.example.order_services.entity.Product.builder().productName("Old").productType("Drink").build();
        product.setId("product-id");
        var variant = com.example.order_services.entity.ProductVariant.builder().product(product)
                .productVariant("Small").price(java.math.BigDecimal.ONE).build();
        variant.setId("variant-id");
        var inventory = Inventory.builder().productVariant(variant).quantityInStock(3).build();
        inventory.setId("different-inventory-id");
        when(inventories.findByProductVariantIdsForUpdate(List.of("variant-id"))).thenReturn(List.of(inventory));
        var request = com.example.order_services.dto.request.UpdateProductInStockRequest.builder()
                .productName("New").productPrice(java.math.BigDecimal.TEN).quantityInStock(20).build();
        var response = service.updateProductInStock("product-id", "variant-id", request);
        assertThat(product.getProductName()).isEqualTo("New");
        assertThat(product.getProductType()).isEqualTo("Drink");
        assertThat(variant.getProductVariant()).isEqualTo("Small");
        assertThat(variant.getId()).isEqualTo("variant-id");
        assertThat(response.getProductPrice()).isEqualByComparingTo("10");
        assertThat(response.getProductStockQuantity()).isEqualTo(20);
        assertThat(response.getProductStockState()).isEqualTo("IN_STOCK");
        assertThatThrownBy(() -> service.updateProductInStock("wrong-product", "variant-id", request))
                .isInstanceOf(ApplicationException.class);
        request.setProductVariantId("new-id");
        assertThatThrownBy(() -> service.updateProductInStock("product-id", "variant-id", request))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void rejectsInvalidStockAndMissingInventory() {
        var request = com.example.order_services.dto.request.UpdateProductInStockRequest.builder()
                .quantityInStock(-1).build();
        assertThatThrownBy(() -> service.updateProductInStock("p", "v", request))
                .isInstanceOf(ApplicationException.class);
        verifyNoInteractions(inventories);
        request.setQuantityInStock(0);
        assertThatThrownBy(() -> service.updateProductInStock("p", "v", request))
                .isInstanceOf(ApplicationException.class);
    }
}
