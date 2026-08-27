package com.example.order_services.repository;

import com.example.order_services.entity.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStateRepository extends JpaRepository<OrderState, String> {
    Optional<OrderState> findByState(String state);
}
