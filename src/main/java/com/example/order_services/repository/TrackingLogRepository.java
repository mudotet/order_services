package com.example.order_services.repository;

import com.example.order_services.entity.TrackingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrackingLogRepository extends JpaRepository<TrackingLog, String> {
    @Query("""
            select log.recipientName from TrackingLog log
            where log.order.id = :orderId and log.newStatus.state = 'DELIVERED' and log.deleted = false
            """)
    Optional<String> findRecipientName(@Param("orderId") String orderId);
}
