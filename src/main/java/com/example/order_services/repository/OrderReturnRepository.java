package com.example.order_services.repository;

import com.example.order_services.entity.OrderReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, String> {

    // Timg kiếm tất cả order return với status All Request
    Page<OrderReturn> findAllByDeletedFalse(Pageable pageable);

    // Tìm kiếm order return theo pageable và trạng thái, chỉ lấy những order return chưa bị xóa.
    Page<OrderReturn> findAllByStatusAndDeletedFalse(String status, Pageable pageable);

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
