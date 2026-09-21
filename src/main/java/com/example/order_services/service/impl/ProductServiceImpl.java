package com.example.order_services.service.impl;

import com.example.order_services.dto.request.AddProductRequest;
import com.example.order_services.dto.request.AddProductVariantRequest;
import com.example.order_services.entity.Product;
import com.example.order_services.entity.ProductVariant;
import com.example.order_services.repository.ProductRepository;
import com.example.order_services.repository.ProductVariantRepository;
import com.example.order_services.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ModelMapper modelMapper;


    // Add new product
    @Override
    public Boolean addProduct(AddProductRequest request) {
        if (productRepository.existsById(request.getProductId())) {
            return false;
        }
        Product new_product = modelMapper.map(request, Product.class);
        try {
            productRepository.save(new_product);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // Add new product variant
    @Override
    public Boolean addProductVariant(String productId, AddProductVariantRequest request) {
        if (!productRepository.existsById(productId) || !request.getProductId().equals(productId)) {
            return false;
        }
        // Implementation for adding product variant
        ProductVariant new_variant = modelMapper.map(request, ProductVariant.class);
        try {
            productVariantRepository.save(new_variant);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
