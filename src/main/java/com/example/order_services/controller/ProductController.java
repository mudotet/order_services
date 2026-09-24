package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.AddProductRequest;
import com.example.order_services.dto.request.AddProductVariantRequest;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;


    // add product
    @PostMapping()
    public BaseResponse<Boolean> addProduct(
            @Valid @RequestBody AddProductRequest request
    ) {
        return BaseResponse.success(productService.addProduct(request));
    }

    // add product variant
    @PostMapping("/{productId}/variants")
    public BaseResponse<Boolean> addProductVariant(
            @PathVariable String productId,
            @Valid @RequestBody AddProductVariantRequest request
    ) {
        return BaseResponse.success(productService.addProductVariant(productId, request));
    }

}
