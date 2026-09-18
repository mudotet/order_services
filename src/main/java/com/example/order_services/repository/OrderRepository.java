package com.example.order_services.repository;

import com.example.order_services.entity.Order;
import com.example.order_services.dto.response.TrackingOrderDetailResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndDeletedFalse(String id);

    @Query("""
            select new com.example.order_services.dto.response.TrackingOrderDetailResponse(
                orders.id, state.state, null, orders.total, address.address, payment.paymentMethod,
                address.city, orders.estimatedDelivery, null)
            from Order orders
            join orders.orderState state
            join Address address on address.id = orders.addressId
            join Payment payment on payment.id = orders.paymentId
            where orders.id = :orderId and orders.user.id = :userId
              and orders.deleted = false and orders.user.deleted = false
            """)
    Optional<TrackingOrderDetailResponse> findTrackingOrderInfo(@Param("orderId") String orderId,
                                                               @Param("userId") String userId);
}
