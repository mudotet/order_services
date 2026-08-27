package com.example.order_services.repository;

import com.example.order_services.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
}
