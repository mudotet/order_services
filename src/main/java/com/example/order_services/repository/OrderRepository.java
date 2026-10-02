package com.example.order_services.repository;

import com.example.order_services.entity.Order;
import com.example.order_services.dto.response.TrackingOrderDetailResponse;
import com.example.order_services.dto.response.ShipperOrderResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    @Query("""
            select o from Order o left join o.shipper s where o.deleted = false and o.user.deleted = false
              and (:userId is null or o.user.id = :userId)
              and (:shipperId is null or s.id = :shipperId)
              and (:state = '' or o.orderState.state = :state)
              and (lower(o.id) like lower(concat('%', :query, '%'))
                   or lower(o.user.userName) like lower(concat('%', :query, '%')))
            """)
    org.springframework.data.domain.Page<Order> browse(String userId, String shipperId, String state,
                                                       String query, org.springframework.data.domain.Pageable pageable);

    Optional<Order> findFirstByIdAndDeletedFalseAndUser_DeletedFalse(String id);

    @Query("""
            select distinct a from Address a, Order o where a.id = o.addressId
              and o.user.id = :userId and o.deleted = false and a.deleted = false
            """)
    java.util.List<com.example.order_services.entity.Address> findUserAddresses(String userId);

    @Query("""
            select distinct p from Payment p, Order o where p.id = o.paymentId
              and o.user.id = :userId and o.deleted = false and p.deleted = false
            """)
    java.util.List<com.example.order_services.entity.Payment> findUserPayments(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndDeletedFalse(String id);

    @Query("""
            select new com.example.order_services.dto.response.ShipperOrderResponse(
                orders.id, orders.orderState.state, orders.deliveryAttemptId,
                orders.user.userName, orders.user.phoneNumber, address.address, address.city,
                coalesce((select sum(item.quantity) from OrderItem item
                    where item.order = orders and item.deleted = false), 0L),
                orders.assignedAt, orders.estimatedDelivery, null,
                (select log.recipientName from TrackingLog log where log.order = orders
                    and log.newStatus.state = 'DELIVERED' and log.deleted = false))
            from Order orders left join Address address on address.id = orders.addressId
            where orders.shipper.id = :shipperId and orders.deleted = false and orders.user.deleted = false
              and orders.id = :orderId
            """)
    Optional<ShipperOrderResponse> findShipperOrder(@Param("orderId") String orderId, @Param("shipperId") String shipperId);

    @Query("""
            select new com.example.order_services.dto.response.TrackingOrderDetailResponse(
                orders.id, state.state, null, orders.total, address.address, payment.paymentMethod,
                address.city, orders.estimatedDelivery, null, null)
            from Order orders
            join orders.orderState state
            join Address address on address.id = orders.addressId
            left join Payment payment on payment.id = orders.paymentId
            where orders.id = :orderId and orders.user.id = :userId
              and orders.deleted = false and orders.user.deleted = false
            """)
    Optional<TrackingOrderDetailResponse> findTrackingOrderInfo(@Param("orderId") String orderId,
                                                               @Param("userId") String userId);
}
