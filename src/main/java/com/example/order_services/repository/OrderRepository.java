package com.example.order_services.repository;

import com.example.order_services.entity.Order;
import com.example.order_services.dto.response.TrackingOrderDetailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("""
            select new com.example.order_services.dto.response.TrackingOrderDetailResponse(
                orders.id, state.state, null, orders.total, address.address, payment.paymentMethod)
            from Order orders
            join orders.orderState state
            left join Address address on address.id = orders.addressId
            left join Payment payment on payment.id = orders.paymentId
            where orders.id = :orderId and orders.user.id = :userId
              and orders.deleted = false and orders.user.deleted = false
            """)
    Optional<TrackingOrderDetailResponse> findTrackingOrderInfo(@Param("orderId") String orderId,
                                                               @Param("userId") String userId);
}
