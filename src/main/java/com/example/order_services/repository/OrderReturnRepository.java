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

    // Use a cursor with descending IDs; returning a List avoids a COUNT query, and the projection avoids loading entities.
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

    // Fetch non-deleted order returns with customer details.
    @EntityGraph(attributePaths = "order.user")
    Page<OrderReturn> findAllByDeletedFalse(Pageable pageable);

    // Search order returns by pagination and status, including only non-deleted returns.
    @EntityGraph(attributePaths = "order.user")
    Page<OrderReturn> findAllByStatusAndDeletedFalse(String status, Pageable pageable);

    // Find a non-deleted order return by ID.
    @EntityGraph(attributePaths = "order.user")
    Optional<OrderReturn> findByIdAndDeletedFalse(String id);

    // Currently sum only APPROVED returns by refund quarter; SUM may return null when there is no data.
    @Query("SELECT SUM(o.refundAmount) FROM OrderReturn o " +
            "WHERE o.status = 'APPROVED' " +
            "AND o.deleted = false " +
            "AND EXTRACT(YEAR FROM o.refundedAt) = :year " +
            "AND EXTRACT(QUARTER FROM o.refundedAt) = :quarter")
    BigDecimal getTotalRefundForSpecificQuarter(@Param("year") int year, @Param("quarter") int quarter);

    // The method name refers to awaiting inspection, but the condition currently counts the INSPECTING status.
    @Query("SELECT COUNT(o) FROM OrderReturn o WHERE o.status = 'INSPECTING' AND o.deleted = false")
    Integer getAwaitInspectionCount();

    @Query("SELECT o FROM OrderReturn o WHERE o.status = 'REFUNDED' AND o.deleted = false AND o.requestedAt != null AND o.refundedAt != null")
    List<OrderReturn> getCompleteReturns();

    // These statuses count as returns still being processed in the statistics.
    @Query("SELECT COUNT(o) FROM OrderReturn o WHERE o.status IN ('PENDING', 'IN_TRANSIT', 'WAREHOUSE_RECEIVED', 'INSPECTING') AND o.deleted = false")
    Integer getActiveReturnCount();



}
