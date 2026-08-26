package com.example.order_services.repository;

import com.example.order_services.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByUserIdAndDeletedFalse(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from Cart cart where cart.userId = :userId and cart.deleted = false")
    Optional<Cart> findForUpdateByUserId(@Param("userId") String userId);
}
