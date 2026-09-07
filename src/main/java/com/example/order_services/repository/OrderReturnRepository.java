package com.example.order_services.repository;

import com.example.order_services.entity.OrderReturn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, String> {
}
