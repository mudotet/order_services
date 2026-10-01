package com.example.order_services.repository;

import com.example.order_services.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    @Query(value = """
            SELECT * FROM notifications
            WHERE processed_at IS NULL AND deleted = 0 AND attempts < 3
            ORDER BY created_at ASC, id ASC
            LIMIT 100
            """, nativeQuery = true)
    List<Notification> findPending();
}
