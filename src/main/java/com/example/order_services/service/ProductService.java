package com.example.order_services.service;

import com.example.order_services.dto.request.AddProductRequest;
import com.example.order_services.dto.request.AddProductVariantRequest;
import jakarta.validation.Valid;

public interface ProductService {

    Boolean addProduct(@Valid AddProductRequest request);

    Boolean addProductVariant(String productId, @Valid AddProductVariantRequest request);
}
