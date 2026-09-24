package com.example.order_services.repository;

import com.example.order_services.entity.OrderItem;
import com.example.order_services.dto.response.PurchasedItemResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    @Query("""
            select new com.example.order_services.dto.response.PurchasedItemResponse(
                product.productName, variant.productVariant, item.quantity, item.unitPrice)
            from OrderItem item
            join item.productVariant variant
            join variant.product product
            where item.order.id = :orderId and item.order.user.id = :userId
              and item.deleted = false and item.order.deleted = false and item.order.user.deleted = false
            order by item.createdAt, item.id
            """)
    List<PurchasedItemResponse> findPurchasedItems(@Param("orderId") String orderId, @Param("userId") String userId);
}
