package com.example.order_services.repository;

import com.example.order_services.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByProductVariantIdAndDeletedFalse(String productVariantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from Inventory inventory "
            + "where inventory.productVariantId = :productVariantId "
            + "and inventory.deleted = false")
    Optional<Inventory> findForUpdateByProductVariantId(
            @Param("productVariantId") String productVariantId
    );
}
