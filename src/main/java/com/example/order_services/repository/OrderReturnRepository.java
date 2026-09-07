package com.example.order_services.repository;

import com.example.order_services.dto.response.OrderReturnResponse;
import com.example.order_services.entity.OrderReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, String> {
    @Query("SELECT SUM(o.refundAmount) FROM OrderReturn o " +
            "WHERE o.status = 'APPROVED' " +
            "AND o.deleted = false " +
            "AND EXTRACT(YEAR FROM o.refundedAt) = :year " +
            "AND EXTRACT(QUARTER FROM o.refundedAt) = :quarter")
    BigDecimal getTotalRefundForSpecificQuarter(@Param("year") int year, @Param("quarter") int quarter);

    @Query("SELECT COUNT(o) FROM OrderReturn o WHERE o.status = 'INSPECTING' AND o.deleted = false")
    Integer getAwaitInspectionCount();

    @Query("SELECT o FROM OrderReturn o WHERE o.status = 'REFUNDED' AND o.deleted = false AND o.requestedAt != null AND o.refundedAt != null")
    List<OrderReturn> getCompleteReturns();

    @Query("SELECT COUNT(o) FROM OrderReturn o WHERE o.status IN ('PENDING', 'IN_TRANSIT', 'WAREHOUSE_RECEIVED', 'INSPECTING') AND o.deleted = false")
    Integer getActiveReturnCount();

}
