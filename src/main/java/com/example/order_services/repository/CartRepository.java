package com.example.order_services.repository;

import com.example.order_services.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByUser_IdAndDeletedFalse(String userId);

    // Use a shared lock for cart updates and order creation; callers need an appropriate transaction to hold the lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.user.id = :userId and c.deleted = false")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") String userId);
}
