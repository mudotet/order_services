package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController {
    private final InventoryService inventoryService;

    @PutMapping("/{productVariantId}/quantity")
    public BaseResponse<InventoryResponse> updateQuantity(
            @PathVariable String productVariantId,
            @Valid @RequestBody UpdateInventoryQuantityRequest request
    ) {
        return BaseResponse.success(inventoryService.updateQuantity(productVariantId, request));
    }
}
