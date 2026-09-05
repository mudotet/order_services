package com.example.order_services.service;

import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;

public interface InventoryService {
    InventoryResponse updateQuantity(
            String productVariantId,
            UpdateInventoryQuantityRequest request
    );
}
