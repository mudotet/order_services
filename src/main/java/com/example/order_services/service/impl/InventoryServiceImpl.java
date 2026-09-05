package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.entity.Inventory;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public InventoryResponse updateQuantity(String productVariantId, UpdateInventoryQuantityRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Inventory quantity must be nonnegative");
        }
        Inventory inventory = inventoryRepository.findByProductVariantIdsForUpdate(List.of(productVariantId)).stream()
                .findFirst().orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found"));
        inventory.setQuantityInStock(request.getQuantity());
        inventoryRepository.save(inventory);
        return new InventoryResponse(inventory.getId(), productVariantId, inventory.getQuantityInStock());
    }
}
