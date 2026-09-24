package com.example.order_services.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.example.order_services.common.DiscountType;
import com.example.order_services.common.EnumCode;
import com.example.order_services.common.OrderStatus;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.UpdateOrderStateRequest;
import com.example.order_services.dto.response.*;
import com.example.order_services.entity.*;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.CurrentUserService;
import com.example.order_services.service.OrderService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Xử lý đặt hàng cho người dùng và quản lý đơn trả hàng cho admin. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final ZoneId DELIVERY_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int RETURN_EXPORT_BATCH_SIZE = 500;


    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserDiscountRepository userDiscountRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderStateRepository orderStateRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final OrderReturnItemRepository orderReturnItemRepository;

    private final CurrentUserService currentUserService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;



    // Lấy thông tin theo dõi đơn hàng thuộc người dùng đang đăng nhập.
    @Override
    @PreAuthorize("hasRole('USER')")
    public TrackingOrderDetailResponse getTrackingOrderInfo(String orderId) {
        String userId = currentUserService.getCurrentUser().getId();
        TrackingOrderDetailResponse tracking = orderRepository.findTrackingOrderInfo(orderId, userId)
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order not found"));
        tracking.setPurchasedItems(orderItemRepository.findPurchasedItems(orderId, userId));
        if (OrderStatus.CANCELLED.name().equals(tracking.getOrderTrackingStatus())) {
            tracking.setEstimatedDelivery(null);
            tracking.setDaysRemaining(null);
        } else if (OrderStatus.DELIVERED.name().equals(tracking.getOrderTrackingStatus())) {
            tracking.setDaysRemaining(0);
        } else if (tracking.getEstimatedDelivery() != null) {
            tracking.setDaysRemaining(Math.toIntExact(Math.max(0, ChronoUnit.DAYS.between(
                    LocalDate.now(DELIVERY_TIME_ZONE), tracking.getEstimatedDelivery()))));
        }
        return tracking;
    }

    // Admin chuyển trạng thái, giữ nguyên ngày giao dự kiến đã lưu.
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void updateOrderState(String id, UpdateOrderStateRequest request) {
        User admin = currentUserService.getCurrentUser();
        Order order = orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order not found"));
        OrderStatus current = OrderStatus.valueOf(order.getOrderState().getState());
        OrderStatus next = request.getState();
        if (next == current) {
            return;
        }
        boolean allowed = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid order state transition");
        }
        OrderState state = orderStateRepository.findByStateAndDeletedFalse(next.name())
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order state not found"));
        order.setOrderState(state);
        order.setUpdatedBy(admin.getId());
        orderRepository.save(order);
    }

    // Tổng hợp số lượng và tiền hoàn của các đơn trả hàng cho admin.
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public OrderReturnsSummaryResponse calculateOrderReturnSummary() {
        currentUserService.getCurrentUser();

        return OrderReturnsSummaryResponse.builder()
                .activeReturnChangePercentage(calActiveReturnChangePercentage())
                .activeReturnCount(calActiveReturnCount())
                .averageCycleTime(calAverageCycleTime())
                .awaitInspectionCount(calAwaitInspectionCount())
                .totalRefunds(calTotalRefunds())
                .build();
    }


    // Lấy tổng tiền hoàn theo quý hiện tại.
    private BigDecimal calTotalRefunds() {
        return orderReturnRepository.getTotalRefundForSpecificQuarter(LocalDateTime.now().getYear(),
                (LocalDateTime.now().getMonthValue() - 1) / 3 + 1);
    }

    // Đếm các đơn đang kiểm tra theo truy vấn thống kê.
    private Integer calAwaitInspectionCount() {
        return orderReturnRepository.getAwaitInspectionCount();
    }

    // Tính số giờ trung bình từ lúc yêu cầu trả hàng đến khi hoàn tiền.
    private Integer calAverageCycleTime() {
        List<OrderReturn> completedReturns = orderReturnRepository.getCompleteReturns();
        if (completedReturns.isEmpty()) {
            return 0;
        }
        long totalHours = completedReturns.stream()
                .mapToLong(orderReturn -> {
                    LocalDateTime requestedAt = orderReturn.getRequestedAt();
                    LocalDateTime refundedAt = orderReturn.getRefundedAt();
                    return java.time.Duration.between(requestedAt, refundedAt).toHours();
                })
                .sum();
        return (int) Math.round((double) totalHours / completedReturns.size());
    }

    // Tạm trả 0 khi chưa triển khai so sánh số đơn trả hàng giữa các kỳ.
    private Integer calActiveReturnChangePercentage() {
        return 0;
    }

    // Đếm các đơn trả hàng vẫn đang được xử lý.
    private Integer calActiveReturnCount() {
        return orderReturnRepository.getActiveReturnCount();
    }

    // Tính tạm tính, giảm giá, phí vận chuyển và tổng tiền từ giỏ của người dùng.
    @Override
    @PreAuthorize("hasRole('USER')")
    public OrderSummaryResponse calculateOrderSummary(String discountId) {
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUser_IdAndDeletedFalse(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty"));
        return loadCheckout(user.getId(), cart, discountId).getSummary();
    }

    // Tạo đơn từ giỏ của người dùng, cập nhật tồn kho và ghi nhận mã giảm giá đã dùng.
    @Override
    @PreAuthorize("hasRole('USER')")
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = currentUserService.getCurrentUser();

        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty"));
        Checkout checkout = loadCheckout(user.getId(), cart, request.getDiscountId());
        Map<String, Long> quantities = new TreeMap<>();
        for (CartItem item : checkout.getItems()) {
            quantities.merge(item.getProductVariant().getId(), item.getProductQuantity().longValue(), Long::sum);
        }
        Map<String, Inventory> inventories = inventoryRepository.findByProductVariantIdsForUpdate(quantities.keySet())
                .stream().collect(Collectors.toMap(inventory -> inventory.getProductVariant().getId(), Function.identity()));
        for (var entry : quantities.entrySet()) {
            Inventory inventory = inventories.get(entry.getKey());
            if (inventory == null) {
                throw new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found");
            }
            if (inventory.getQuantityInStock() < entry.getValue()) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Insufficient stock");
            }
        }

        OrderState pending = orderStateRepository.findByStateAndDeletedFalse(OrderStatus.PENDING.name())
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "PENDING order state not found"));
        OrderSummaryResponse summary = checkout.getSummary();
        Order order = Order.builder()
                .user(user)
                .discount(checkout.getAssignment() == null ? null : checkout.getAssignment().getDiscount())
                .addressId(request.getAddressId())
                .paymentId(request.getPaymentId())
                .orderState(pending)
                .subtotal(summary.getSubtotal())
                .discountAmount(summary.getDiscountAmount())
                .shippingFee(summary.getShippingFee())
                .total(summary.getTotal())
                .build();
        orderRepository.save(order);
        List<OrderItem> orderItems = checkout.getItems().stream().map(item -> OrderItem.builder()
                .order(order)
                .productVariant(item.getProductVariant())
                .quantity(item.getProductQuantity())
                .unitPrice(item.getProductVariant().getPrice())
                .lineTotal(calculateLineTotal(item))
                .build()).toList();
        orderItemRepository.saveAll(orderItems);

        for (var entry : quantities.entrySet()) {
            Inventory inventory = inventories.get(entry.getKey());
            inventory.setQuantityInStock(inventory.getQuantityInStock() - entry.getValue().intValue());
        }
        inventoryRepository.saveAll(inventories.values());
        checkout.getItems().forEach(item -> item.setDeleted(true));
        cartItemRepository.saveAll(checkout.getItems());
        if (checkout.getAssignment() != null) {
            checkout.getAssignment().setUsedAt(LocalDateTime.now());
            checkout.getAssignment().setStatus("USED");
            userDiscountRepository.save(checkout.getAssignment());
        }
        return OrderResponse.builder()
                .id(order.getId())
                .state(pending.getState())
                .discountAmount(summary.getDiscountAmount())
                .subtotal(summary.getSubtotal())
                .shippingFee(summary.getShippingFee())
                .total(summary.getTotal())
                .build();
    }

    // Chuẩn bị giỏ hàng, kiểm tra mã giảm giá và tính tiền dùng chung cho xem trước và tạo đơn.
    private Checkout loadCheckout(String userId, Cart cart, String discountId) {
        List<CartItem> items = cartItemRepository.findActiveItemsByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty");
        }
        BigDecimal subtotal = ZERO_MONEY;
        for (CartItem item : items) {
            if (item.getProductQuantity() == null || item.getProductQuantity() <= 0
                    || item.getProductVariant().getPrice() == null || item.getProductVariant().getPrice().signum() < 0) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid cart item");
            }
            subtotal = subtotal.add(calculateLineTotal(item));
        }
        UserDiscount assignment = null;
        BigDecimal discountAmount = ZERO_MONEY;
        if (discountId != null && !discountId.isBlank()) {
            assignment = userDiscountRepository.findAvailableAssignment(userId, discountId)
                    .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Discount unavailable"));
            Discount discount = assignment.getDiscount();
            BigDecimal discountValue = discount.getDiscountValue();
            if (discountValue == null || discountValue.signum() < 0 || discount.getDiscountType() == null
                    || (discount.getDiscountType() == DiscountType.PERCENTAGE && discountValue.compareTo(new BigDecimal("100")) > 0)) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid discount value");
            }
            if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
                discountAmount = subtotal.multiply(discountValue)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else {
                discountAmount = discountValue;
            }
            discountAmount = discountAmount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal shippingFee = new BigDecimal("30000.00");
        BigDecimal total = subtotal.subtract(discountAmount).add(shippingFee);
        OrderSummaryResponse summary = new OrderSummaryResponse(subtotal, discountAmount, shippingFee, total);
        return new Checkout(items, assignment, summary);
    }

    // Tính thành tiền một dòng giỏ hàng, làm tròn đến hai chữ số thập phân.
    private BigDecimal calculateLineTotal(CartItem item) {
        return item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Getter
    @RequiredArgsConstructor
    private static class Checkout {
        private final List<CartItem> items;
        private final UserDiscount assignment;
        private final OrderSummaryResponse summary;
    }

    // Lấy danh sách đơn trả hàng cho admin, hỗ trợ phân trang và lọc theo trạng thái.
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Page<OrderReturnResponse> getOrderReturns(int page, int size, String filterBy) {
        currentUserService.getCurrentUser();
        if (page < 0 || size < 1 || size > 100) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Page must be nonnegative and size must be between 1 and 100");
        }
        String status = normalizeReturnFilter(filterBy);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<OrderReturn> returns = status.equals("ALL_REQUESTS")
                ? orderReturnRepository.findAllByDeletedFalse(pageable)
                : orderReturnRepository.findAllByStatusAndDeletedFalse(status, pageable);
        Map<String, List<OrderReturnItem>> itemsByReturn = returns.isEmpty() ? Map.of()
                : orderReturnItemRepository.findByOrderReturnIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(
                        returns.getContent().stream().map(OrderReturn::getId).toList()).stream()
                .collect(Collectors.groupingBy(item -> item.getOrderReturn().getId()));
        return returns.map(orderReturn -> {
            OrderReturnResponse response = new OrderReturnResponse();
            populateReturnResponse(orderReturn, itemsByReturn.getOrDefault(orderReturn.getId(), List.of()), response);
            return response;
        });
    }

    // Ghi từng batch vào file CSV, dùng ID cuối batch làm cursor tiếp theo.
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Path exportOrderReturns(String filterBy) throws IOException {
        currentUserService.getCurrentUser();
        String filter = normalizeReturnFilter(filterBy);
        String status = filter.equals("ALL_REQUESTS") ? null : filter;
        LocalDateTime exportTime = LocalDateTime.now();
        Path file = Files.createTempFile("order-returns-", ".csv");
        try {
            try (ExcelWriter writer = EasyExcel.write(file.toFile(), ExportOrderReturn.class)
                    .excelType(ExcelTypeEnum.CSV).charset(StandardCharsets.UTF_8).withBom(true).build()) {
                WriteSheet sheet = EasyExcel.writerSheet().build();
                writer.write(List.of(), sheet);
                String cursorId = null;
                while (true) {
                    List<ExportOrderReturn> batch = orderReturnRepository.findExportBatch(
                            status, cursorId, PageRequest.of(0, RETURN_EXPORT_BATCH_SIZE));
                    if (batch.isEmpty()) {
                        break;
                    }
                    Map<String, Set<String>> reasons = new HashMap<>();
                    for (Object[] reason : orderReturnItemRepository.findExportReasons(
                            batch.stream().map(ExportOrderReturn::getReturnId).toList())) {
                        reasons.computeIfAbsent((String) reason[0], key -> new LinkedHashSet<>()).add((String) reason[1]);
                    }
                    for (ExportOrderReturn row : batch) {
                        row.setInitialTime(Math.max(0, Duration.between(row.getCreatedAt(), exportTime).toMinutes()));
                        row.setReasonReturn(String.join(", ", reasons.getOrDefault(row.getReturnId(), Set.of())));
                    }
                    writer.write(batch, sheet);
                    cursorId = batch.getLast().getReturnId();
                }
            }
            return file;
        } catch (RuntimeException | Error failure) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    private String normalizeReturnFilter(String filterBy) {
        String status = filterBy == null ? "" : filterBy.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        if (status.equals("ALLREQUEST") || status.equals("ALLREQUESTS")) {
            status = "ALL_REQUESTS";
        }
        if (!Set.of("ALL_REQUESTS", "PENDING", "IN_TRANSIT", "WAREHOUSE_RECEIVED", "RESTOCKED", "REFUNDED").contains(status)) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Unsupported return status filter");
        }
        return status;
    }

    // Lấy chi tiết đơn trả hàng và các sản phẩm trả lại cho admin.
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ViewOrderDetailResponse viewOrderReturnDetail(String id) {
        currentUserService.getCurrentUser();
        OrderReturn orderReturn = orderReturnRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order return not found"));
        List<OrderReturnItem> items = orderReturnItemRepository
                .findByOrderReturnIdInAndDeletedFalseOrderByCreatedAtAscIdAsc(List.of(id));
        ViewOrderDetailResponse response = new ViewOrderDetailResponse();
        populateReturnResponse(orderReturn, items, response);
        response.setItems(items.stream().map(item -> OrderReturnItemResponse.builder()
                .orderItemId(item.getOrderItem().getId())
                .productVariantId(item.getOrderItem().getProductVariant().getId())
                .productName(item.getOrderItem().getProductVariant().getProduct().getProductName())
                .reasonType(item.getReasonType())
                .quantity(item.getQuantity())
                .conditionStatus(item.getConditionStatus())
                .unitPrice(item.getUnitPrice())
                .refundAmount(item.getRefundAmount())
                .build()).toList());
        return response;
    }

    // Chuyển thông tin chung của đơn trả hàng sang DTO dùng cho danh sách và chi tiết.
    private void populateReturnResponse(OrderReturn orderReturn, List<OrderReturnItem> items, OrderReturnResponse response) {
        LocalDateTime createdAt = orderReturn.getCreatedAt();
        long minutes = createdAt == null ? 0 : Math.max(0, Duration.between(createdAt, LocalDateTime.now()).toMinutes());
        response.setReturnId(orderReturn.getId());
        response.setInitialTime((int) Math.min(Integer.MAX_VALUE, minutes));
        response.setCustomerName(orderReturn.getOrder().getUser().getUserName());
        response.setReasonReturn(items.stream().map(OrderReturnItem::getReasonType).distinct().collect(Collectors.joining(", ")));
        response.setOriginType(orderReturn.getOriginType());
        response.setOrderReturnStatus(orderReturn.getStatus());
    }
}
