package com.example.order_services.service;

import com.example.order_services.entity.Notification;
import com.example.order_services.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationJobTest {
    @Test
    void processesSuccessfulNotificationAndSkipsCompletedRows() {
        NotificationRepository repository = mock(NotificationRepository.class);
        Notification notification = Notification.builder().orderId("order-1").message("SHIPPING").build();
        notification.setId("notification-1");
        when(repository.findPending())
                .thenReturn(List.of(notification));
        when(repository.findById("notification-1")).thenReturn(Optional.of(notification));
        NotificationJob job = new NotificationJob(repository, transactions());

        job.processNotifications();
        job.processNotifications();

        assertNotNull(notification.getProcessedAt());
        assertEquals(1, notification.getAttempts());
        verify(repository, times(1)).saveAndFlush(notification);
    }

    @Test
    void blocksLaterNotificationUntilEarlierOneExhaustsThreeAttempts() {
        NotificationRepository repository = mock(NotificationRepository.class);
        Notification shipping = spy(Notification.builder().orderId("order-1").message("SHIPPING").build());
        shipping.setId("shipping");
        doThrow(new IllegalStateException("Output unavailable")).when(shipping).getCustomerName();
        Notification delivered = Notification.builder().orderId("order-1").message("DELIVERED").build();
        delivered.setId("delivered");
        when(repository.findPending())
                .thenReturn(List.of(shipping, delivered));
        when(repository.findById("shipping")).thenReturn(Optional.of(shipping));
        when(repository.findById("delivered")).thenReturn(Optional.of(delivered));
        NotificationJob job = new NotificationJob(repository, transactions());

        job.processNotifications();
        job.processNotifications();
        assertNull(delivered.getProcessedAt());
        assertEquals(2, shipping.getAttempts());
        job.processNotifications();

        assertEquals(3, shipping.getAttempts());
        assertNull(shipping.getProcessedAt());
        assertTrue(shipping.getLastError().contains("Output unavailable"));
        assertNotNull(delivered.getProcessedAt());
    }

    private TransactionTemplate transactions() {
        TransactionTemplate transactions = mock(TransactionTemplate.class);
        when(transactions.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        return transactions;
    }
}
