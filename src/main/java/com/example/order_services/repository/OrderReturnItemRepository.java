package com.example.order_services.repository;

import com.example.order_services.entity.OrderReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface OrderReturnItemRepository extends JpaRepository<OrderReturnItem, String> {
    List<OrderReturnItem> findByOrderReturnId(String id);
}
