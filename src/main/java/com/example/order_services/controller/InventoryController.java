package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.request.UpdateProductInStockRequest;
import com.example.order_services.dto.response.GeneralnventoryResponse;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.dto.response.ProductInventoryResponse;
import com.example.order_services.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;

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

    // Search inventory by product name, including out-of-stock variants.
    @GetMapping("/productsInStock")
    public BaseResponse<Page<ProductInventoryResponse>> getProductsInventoryInStockWithQuery(
            @RequestParam(name = "query", defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page) {
        return BaseResponse.success(inventoryService.getProductsInventoryInStockWithQuery(query, page));
    }

    // Update product in Stock
    @PatchMapping("/productsInStock/{productId}/variants/{variantsId}")
    public BaseResponse<ProductInventoryResponse> updateProductInStock(@PathVariable String productId,
                                                                       @PathVariable String variantsId,
                                                                       @Valid @RequestBody UpdateProductInStockRequest updateProductInStockRequest){
        return BaseResponse.success(inventoryService.updateProductInStock(productId, variantsId, updateProductInStockRequest));
    }
}
