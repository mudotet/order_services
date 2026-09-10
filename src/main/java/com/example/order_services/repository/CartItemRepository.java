package com.example.order_services.repository;

import com.example.order_services.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    // Nạp sẵn biến thể/sản phẩm để đọc giá và tên; bỏ các dòng có mục giỏ, biến thể hoặc sản phẩm bị xóa mềm.
    @Query("""
            select item from CartItem item
            join fetch item.productVariant variant
            join fetch variant.product product
            where item.cart.id = :cartId and item.deleted = false
              and variant.deleted = false and product.deleted = false
            order by variant.id
            """)
    List<CartItem> findActiveItemsByCartId(@Param("cartId") String cartId);
}
