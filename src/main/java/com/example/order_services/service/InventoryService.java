package com.example.order_services.service;

import com.example.order_services.dto.request.UpdateQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;

public interface InventoryService {
    InventoryResponse updateQuantity(
            String productVariantId,
            UpdateQuantityRequest request
    );
}
