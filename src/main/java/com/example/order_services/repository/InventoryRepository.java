package com.example.order_services.repository;

import com.example.order_services.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
    @EntityGraph(attributePaths = "productVariant")
    List<Inventory> findAllByProductVariantIdInAndDeletedFalse(Collection<String> productVariantIds);

    @Query("""
            select inventory from Inventory inventory
            join fetch inventory.productVariant variant
            where variant.id in :productVariantIds and inventory.deleted = false
              and variant.deleted = false and variant.product.deleted = false
            order by variant.id
            """)
    List<Inventory> findByProductVariantIdsForUpdate(@Param("productVariantIds") Collection<String> productVariantIds);

    @Query(
            """
            select sum(inventory.quantityInStock * variant.price) from Inventory inventory
            join inventory.productVariant variant
            where inventory.deleted = false and variant.deleted = false and variant.product.deleted = false
            """
    )
    BigDecimal countTotalInventoryValue();


    @Query("""
             select sum(inventory.quantityInStock) from Inventory inventory
            where inventory.deleted = false
            """
    )
    BigInteger countTotalProductInInventory();

    @Query(
            """
            select count(variant.id) from Inventory inventory
            join inventory.productVariant variant
            where inventory.deleted = false and variant.deleted = false and variant.product.deleted = false and inventory.quantityInStock <=20
            """
    )
    BigInteger countTotalProductAboutToOutOfStock();
}
