package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.dto.request.UpdateQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.entity.Inventory;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public InventoryResponse updateQuantity(
            String productVariantId,
            UpdateQuantityRequest request
    ) {
        Inventory inventory = inventoryRepository
                .findByProductVariantId(productVariantId)
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.NOT_FOUND,
                        "Inventory not found"
                ));
        inventory.setQuantityInStock(request.getQuantity());
        return modelMapper.map(inventoryRepository.save(inventory), InventoryResponse.class);
    }
}
