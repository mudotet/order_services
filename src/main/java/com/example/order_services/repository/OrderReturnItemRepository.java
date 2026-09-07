package com.example.order_services.repository;

import com.example.order_services.entity.OrderReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderReturnItemRepository extends JpaRepository<OrderReturnItem, String> {
}
