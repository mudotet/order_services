package com.example.order_services.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderViewResponse(String orderId, String state, BigDecimal total, LocalDateTime createdAt,
                                String customerName, String recipientName, String shippingAddress,
                                String shipperId, String deliveryAttemptId, List<Item> items) {
    public record Item(String productName, Integer quantity, BigDecimal unitPrice) {}
    public record Shipper(String userId, String userName, String email) {}
    public record Catalog(String productVariantId, String variantId, String productName, String description,
                          BigDecimal price, Integer quantityInStock) {}
    public record AddressOption(String addressId, String address, String city) {}
    public record PaymentOption(String paymentId, String paymentMethod) {}
}
