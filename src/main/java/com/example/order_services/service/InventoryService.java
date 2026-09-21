package com.example.order_services.service;

import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.request.UpdateProductInStockRequest;
import com.example.order_services.dto.response.GeneralnventoryResponse;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.dto.response.ProductInventoryResponse;

import org.springframework.data.domain.Page;

public interface InventoryService {
    InventoryResponse updateQuantity(
            String productVariantId,
            UpdateInventoryQuantityRequest request
    );

    GeneralnventoryResponse getGeneralInventoryInfo();

    Page<ProductInventoryResponse> getProductsInventoryInStockWithQuery(String query, int page);

    ProductInventoryResponse updateProductInStock(String productId, String variantsId, UpdateProductInStockRequest updateProductInStockRequest);
}
