package com.example.order_services.repository;

import com.example.order_services.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findAllByCartId(String cartId);
}
