package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.response.GeneralnventoryResponse;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.dto.response.ProductInventoryResponse;
import com.example.order_services.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
public class InventoryController {
    private final InventoryService inventoryService;



    // Update the quantity of a product variant in the inventory
    @PutMapping("/{productVariantId}/quantity")
    public BaseResponse<InventoryResponse> updateQuantity(
            @PathVariable String productVariantId,
            @Valid @RequestBody UpdateInventoryQuantityRequest request) {
        return BaseResponse.success(inventoryService.updateQuantity(productVariantId, request));
    }

    // General Inventory Infomation
    @GetMapping()
    public BaseResponse<GeneralnventoryResponse> getGeneralInventoryInfo() {
        return BaseResponse.success(inventoryService.getGeneralInventoryInfo());
    }

    // Get products in stock with filter
    @GetMapping("/productsInStock")
    public BaseResponse<List<ProductInventoryResponse>> getProductsInventoryWithFilter(@RequestParam(name = "query",
            required = false, defaultValue = "") String query, Page){
        return BaseResponse.success(inventoryService.getProductsInventoryInStockWithFilter(filter));
    }
}
