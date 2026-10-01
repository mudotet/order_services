package com.example.order_services.service;

import com.example.order_services.entity.Notification;
import com.example.order_services.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationJob {
    private final NotificationRepository notifications;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public synchronized void processNotifications() {
        Set<String> blockedOrders = new HashSet<>();
        for (Notification pending : notifications.findPending()) {
            if (blockedOrders.contains(pending.getOrderId())) {
                continue;
            }
            try {
                Boolean retryPending = transactions.execute(transaction -> {
                    Notification notification = notifications.findById(pending.getId()).orElseThrow();
                    if (notification.isDeleted() || notification.getProcessedAt() != null || notification.getAttempts() >= 3) {
                        return false;
                    }
                    notification.setAttempts(notification.getAttempts() + 1);
                    try {
                        log.info("[Notification] Order: {} | Customer: {} | Email: {} | Message: {}",
                                notification.getOrderId(), notification.getCustomerName(),
                                notification.getCustomerEmail(), notification.getMessage());
                        notification.setProcessedAt(LocalDateTime.now());
                        notification.setLastError(null);
                    } catch (RuntimeException exception) {
                        String error = exception.toString();
                        notification.setLastError(error.substring(0, Math.min(error.length(), 500)));
                        log.error("Notification {} failed on attempt {}", notification.getId(),
                                notification.getAttempts(), exception);
                    }
                    notifications.saveAndFlush(notification);
                    return notification.getProcessedAt() == null && notification.getAttempts() < 3;
                });
                if (Boolean.TRUE.equals(retryPending)) {
                    blockedOrders.add(pending.getOrderId());
                }
            } catch (RuntimeException exception) {
                log.error("Could not persist notification {}; stopping this batch", pending.getId(), exception);
                return;
            }
        }
    }
}
