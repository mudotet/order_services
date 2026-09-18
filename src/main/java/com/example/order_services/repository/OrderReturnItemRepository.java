package com.example.order_services.repository;

import com.example.order_services.entity.OrderReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderReturnItemRepository extends JpaRepository<OrderReturnItem, String> {
    // Chỉ lấy ID và lý do, không giữ các entity của toàn bộ file trong persistence context.
    @Query("""
            SELECT i.orderReturn.id, i.reasonType FROM OrderReturnItem i
            WHERE i.orderReturn.id IN :returnIds AND i.deleted = false
            ORDER BY i.createdAt ASC, i.id ASC
            """)
    List<Object[]> findExportReasons(@Param("returnIds") Collection<String> returnIds);

    List<OrderReturnItem> findByOrderReturnId(String id);

    // Lấy các sản phẩm trả lại chưa xóa theo danh sách returnId.
    @EntityGraph(attributePaths = {"orderReturn", "orderItem.productVariant.product"})
    List<OrderReturnItem> findByOrderReturnIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(Collection<String> returnIds);
}
