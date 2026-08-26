package com.example.order_services.service;

import com.example.order_services.dto.request.UpdateQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.entity.Inventory;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    @Mock
    private InventoryRepository inventoryRepository;

    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository, new ModelMapper());
    }

    @Test
    void updateQuantitySetsTheExactQuantity() {
        Inventory inventory = new Inventory();
        inventory.setProductVariantId("variant-1");
        inventory.setQuantityInStock(3);
        when(inventoryRepository.findByProductVariantIdAndDeletedFalse("variant-1"))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(inventory)).thenReturn(inventory);

        InventoryResponse response = inventoryService.updateQuantity(
                "variant-1",
                new UpdateQuantityRequest(12)
        );

        assertThat(response.getQuantityInStock()).isEqualTo(12);
        verify(inventoryRepository).save(inventory);
    }
}
