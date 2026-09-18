package com.example.order_services.repository;

import com.example.order_services.entity.OrderReturn;
import com.example.order_services.dto.response.ExportOrderReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, String> {

    // Cursor theo ID giảm dần; List tránh truy vấn COUNT và không tải entity.
    @Query("""
            SELECT new com.example.order_services.dto.response.ExportOrderReturn(
                r.id, r.createdAt, u.userName, r.originType, r.status)
            FROM OrderReturn r JOIN r.order o JOIN o.user u
            WHERE r.deleted = false AND (:status IS NULL OR r.status = :status)
              AND (:cursorId IS NULL OR r.id < :cursorId)
            ORDER BY r.id DESC
            """)
    List<ExportOrderReturn> findExportBatch(@Param("status") String status,
                                          @Param("cursorId") String cursorId,
                                          Pageable pageable);

    // Lấy danh sách đơn trả hàng chưa xóa, kèm thông tin khách hàng.
    @EntityGraph(attributePaths = "order.user")
    Page<OrderReturn> findAllByDeletedFalse(Pageable pageable);

    // Tìm kiếm order return theo pageable và trạng thái, chỉ lấy những order return chưa bị xóa.
    @EntityGraph(attributePaths = "order.user")
    Page<OrderReturn> findAllByStatusAndDeletedFalse(String status, Pageable pageable);

    // Tìm đơn trả hàng chưa xóa theo ID.
    @EntityGraph(attributePaths = "order.user")
    Optional<OrderReturn> findByIdAndDeletedFalse(String id);

    // Hiện chỉ cộng trạng thái APPROVED theo quý hoàn tiền; SUM có thể trả null khi không có dữ liệu.
    @Query("SELECT SUM(o.refundAmount) FROM OrderReturn o " +
            "WHERE o.status = 'APPROVED' " +
            "AND o.deleted = false " +
            "AND EXTRACT(YEAR FROM o.refundedAt) = :year " +
            "AND EXTRACT(QUARTER FROM o.refundedAt) = :quarter")
    BigDecimal getTotalRefundForSpecificQuarter(@Param("year") int year, @Param("quarter") int quarter);

    // Tên phương thức là await inspection, nhưng điều kiện hiện đếm trạng thái INSPECTING.
    @Query("SELECT COUNT(o) FROM OrderReturn o WHERE o.status = 'INSPECTING' AND o.deleted = false")
    Integer getAwaitInspectionCount();

    @Query("SELECT o FROM OrderReturn o WHERE o.status = 'REFUNDED' AND o.deleted = false AND o.requestedAt != null AND o.refundedAt != null")
    List<OrderReturn> getCompleteReturns();

    // Các trạng thái này được xem là lượt trả còn đang xử lý trong thống kê.
    @Query("SELECT COUNT(o) FROM OrderReturn o WHERE o.status IN ('PENDING', 'IN_TRANSIT', 'WAREHOUSE_RECEIVED', 'INSPECTING') AND o.deleted = false")
    Integer getActiveReturnCount();



}
