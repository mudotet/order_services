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
}
