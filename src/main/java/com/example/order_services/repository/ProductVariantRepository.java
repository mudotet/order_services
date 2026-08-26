package com.example.order_services.repository;

import com.example.order_services.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    Optional<ProductVariant> findByIdAndDeletedFalse(String id);
}
