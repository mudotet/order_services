package com.example.order_services.repository;

import com.example.order_services.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, String> {
    List<Discount> findAllByDeletedFalse();

    Optional<Discount> findByIdAndDeletedFalse(String id);
}
