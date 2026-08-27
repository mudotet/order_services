package com.example.order_services.repository;

import com.example.order_services.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountRepository extends JpaRepository<Discount, String> {
}
