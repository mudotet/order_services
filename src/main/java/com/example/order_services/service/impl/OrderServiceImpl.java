package com.example.order_services.service.impl;

import com.example.order_services.common.DiscountType;
import com.example.order_services.common.EnumCode;
import com.example.order_services.common.OrderStatus;
import com.example.order_services.common.OrderReturnStatus;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.response.*;
import com.example.order_services.entity.*;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.CurrentUserService;
import com.example.order_services.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Điều phối thống kê trả hàng, tính tiền giỏ hàng và tạo đơn mua hàng. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final Set<OrderReturnStatus> FILTERABLE_RETURN_STATUSES = EnumSet.of(
            OrderReturnStatus.PENDING,
            OrderReturnStatus.IN_TRANSIT,
            OrderReturnStatus.WAREHOUSE_RECEIVED,
            OrderReturnStatus.RESTOCKED,
            OrderReturnStatus.REFUNDED
    );


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


    /** Tổng hợp số liệu trả hàng; hiện quyền truy cập được kiểm tra bằng username "admin". */
    @Override
    public OrderReturnsSummaryResponse calculateOrderReturnSummary() {
        User user = currentUserService.getCurrentUser();
        if (!user.getUserName().equals("admin")){
            throw new ApplicationException(EnumCode.UNAUTHORIZED, "Unauthorized");
        }

        return OrderReturnsSummaryResponse.builder()
                .activeReturnChangePercentage(calActiveReturnChangePercentage())
                .activeReturnCount(calActiveReturnCount())
                .averageCycleTime(calAverageCycleTime())
                .awaitInspectionCount(calAwaitInspectionCount())
                .totalRefunds(calTotalRefunds())
                .build();
    }


    // Xác định quý hiện tại (1–4); repository cộng tiền theo refundedAt và trạng thái APPROVED.
    private BigDecimal calTotalRefunds() {
        return orderReturnRepository.getTotalRefundForSpecificQuarter(LocalDateTime.now().getYear(),
                (LocalDateTime.now().getMonthValue() - 1) / 3 + 1);
    }

    private Integer calAwaitInspectionCount() {
        return orderReturnRepository.getAwaitInspectionCount();
    }

    // Trung bình số giờ từ yêu cầu đến hoàn tiền của các lượt trả REFUNDED có đủ hai mốc thời gian.
    private Integer calAverageCycleTime() {
        List<OrderReturn> completedReturns = orderReturnRepository.getCompleteReturns();
        if (completedReturns.isEmpty()) {
            return 0;
        }
        // Mỗi lượt được lấy số giờ nguyên trước khi tính trung bình và làm tròn kết quả.
        long totalHours = completedReturns.stream()
                .mapToLong(orderReturn -> {
                    LocalDateTime requestedAt = orderReturn.getRequestedAt();
                    LocalDateTime refundedAt = orderReturn.getRefundedAt();
                    return java.time.Duration.between(requestedAt, refundedAt).toHours();
                })
                .sum();
        return (int) Math.round((double) totalHours / completedReturns.size());
    }

    // Chưa rõ logic
    private Integer calActiveReturnChangePercentage() {
        return 0;
    }

    // Lấy những đơn return đang active
    private Integer calActiveReturnCount() {
        return orderReturnRepository.getActiveReturnCount();
    }

    /** Danh sách trả hàng mới nhất trước; page bắt đầu từ 0, size phải lớn hơn 0. */
    @Override
    public Page<OrderReturnResponse> getOrderReturns(int page, int size, String filterBy) {
        User user = currentUserService.getCurrentUser();
        if(!user.getUserName().equals("admin")){
            throw new ApplicationException(EnumCode.UNAUTHORIZED, "Unauthorized");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<OrderReturn> orderReturnsPages = findOrderReturns(filterBy, pageable);
        // Gom ID khách hàng trong trang hiện tại để tải theo lô và tra cứu bằng userMap.
        Set<String> userIds = orderReturnsPages.stream()
                .map(orderReturn -> orderReturn.getOrder().getUser().getId())
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        // Page.map chuyển entity sang DTO đồng thời giữ tổng số bản ghi và thông tin phân trang.
        return orderReturnsPages.map(orderReturn ->{
            User returnUser = userMap.get(orderReturn.getOrder().getUser().getId());
            LocalDateTime createdAt = orderReturn.getCreatedAt();

            OrderReturnResponse orderReturnResponse = new OrderReturnResponse();

            orderReturnResponse.setReturnId(orderReturn.getId());
            orderReturnResponse.setInitialTime(orderReturnResponse.calculateInitialTime(createdAt));
            orderReturnResponse.setCustomerName(returnUser.getUserName());
            // Ghép lý do của các mặt hàng trong cùng lượt trả; hiện mỗi lượt trả có một truy vấn riêng.
            orderReturnResponse.setReasonReturn(orderReturnItemRepository.findByOrderReturnId(orderReturn.getId()).stream()
                    .map(OrderReturnItem::getReasonDetail)
                    .collect(Collectors.joining(", ")));
            orderReturnResponse.setOriginType(orderReturn.getOriginType());
            orderReturnResponse.setOrderReturnStatus(
                    orderReturn.getStatus()
            );
            return orderReturnResponse;
          });
    }

    private Page<OrderReturn> findOrderReturns(String filterBy, Pageable pageable) {
        String normalizedFilter = normalizeFilter(filterBy);
        if (normalizedFilter.equals("ALL_REQUESTS")) {
            return orderReturnRepository.findAllByDeletedFalse(pageable);
        }

        try {
            OrderReturnStatus status = OrderReturnStatus.valueOf(normalizedFilter);
            if (!FILTERABLE_RETURN_STATUSES.contains(status)) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid return order filter");
            }
            return orderReturnRepository.findAllByStatusAndDeletedFalse(status.name(), pageable);
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid return order filter");
        }
    }

    private String normalizeFilter(String filterBy) {
        if (filterBy == null || filterBy.isBlank()) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid return order filter");
        }
        return filterBy.trim()
                .toUpperCase()
                // thay đổi các ký tự đặc biệt về _ duy nhất
                .replaceAll("[\\s-]+", "_");
    }


    /** Danh sách chi tiết Order Return (nơi chứa Order Return Item) */
    @Override
    public ViewOrderDetailResponse viewOrderReturnDetail(String id) {
        User user = currentUserService.getCurrentUser();
        if(!user.getUserName().equals("admin")){
            throw new ApplicationException(EnumCode.UNAUTHORIZED, "Unauthorized");
        }
        // finding order return by id
        OrderReturn orderReturn = orderReturnRepository.findById(id)
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Order Return not found"));
        List<OrderReturnItem> orderReturnItems = orderReturnItemRepository.findByOrderReturnId(id);

        List<OrderReturnItemResponse> orderReturnItemResponses = orderReturnItems.stream()
                .map(orderReturnItem -> modelMapper.map(orderReturnItem, OrderReturnItemResponse.class))
                .toList();
        return ViewOrderDetailResponse.builder()
                .items(orderReturnItemResponses)
                .build();
    }

    @Override
    public String exportOrderReturnsToCsv() {
        return "";
    }

    /** Xem trước số tiền thanh toán bằng cùng công thức với tạo đơn, chưa ghi đơn hàng. */
    @Override
    public OrderSummaryResponse calculateOrderSummary(String discountId) {
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUser_IdAndDeletedFalse(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty"));
        return loadCheckout(user.getId(), cart, discountId).summary();
    }

    /** Tạo đơn từ giỏ hiện tại, cập nhật tồn kho và đánh dấu các mục giỏ/mã giảm giá đã sử dụng. */
    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = currentUserService.getCurrentUser();
        // Lưu ý: createOrder hiện kế thừa transaction readOnly của lớp; transaction ghi đang được hoãn như ghi chú bên dưới.
        // ponytail: write transaction deferred for learning; restore it before processing real orders.
        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty"));
        // 1. Kiểm tra giỏ và mã giảm giá, tính lại số tiền từ dữ liệu phía server.
        Checkout checkout = loadCheckout(user.getId(), cart, request.getDiscountId());
        // 2. Cộng dồn các dòng cùng biến thể để kiểm tra tổng nhu cầu tồn kho.
        Map<String, Long> quantities = new TreeMap<>();
        for (CartItem item : checkout.items()) {
            quantities.merge(item.getProductVariant().getId(), item.getProductQuantity().longValue(), Long::sum);
        }
        // Truy vấn yêu cầu khóa ghi các bản ghi kho; kiểm tra đủ toàn bộ hàng trước bước lưu đơn.
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

        // 3. Tạo đơn PENDING và lưu giá tại thời điểm mua vào từng OrderItem.
        OrderState pending = orderStateRepository.findByStateAndDeletedFalse(OrderStatus.PENDING.name())
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "PENDING order state not found"));
        OrderSummaryResponse summary = checkout.summary();
        Order order = Order.builder()
                .user(user)
                .discount(checkout.assignment() == null ? null : checkout.assignment().getDiscount())
                .addressId(request.getAddressId())
                .paymentId(request.getPaymentId())
                .orderState(pending)
                .subtotal(summary.getSubtotal())
                .discountAmount(summary.getDiscountAmount())
                .shippingFee(summary.getShippingFee())
                .total(summary.getTotal())
                .build();
        orderRepository.save(order);
        List<OrderItem> orderItems = checkout.items().stream().map(item -> OrderItem.builder()
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
        // 4. Lưu lượng tồn đã trừ và xóa mềm các dòng giỏ đã chuyển thành đơn hàng.
        inventoryRepository.saveAll(inventories.values());
        checkout.items().forEach(item -> item.setDeleted(true));
        cartItemRepository.saveAll(checkout.items());
        // 5. Đánh dấu lượt cấp mã giảm giá đã dùng để không xuất hiện trong truy vấn AVAILABLE.
        if (checkout.assignment() != null) {
            checkout.assignment().setUsed(true);
            checkout.assignment().setStatus("USED");
            userDiscountRepository.save(checkout.assignment());
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

    /** Dùng chung cho xem trước và tạo đơn: kiểm tra dữ liệu, tính tạm tính, giảm giá và phí giao hàng. */
    private Checkout loadCheckout(String userId, Cart cart, String discountId) {
        List<CartItem> items = cartItemRepository.findActiveItemsByCartId(cart.getId());
        if (items.isEmpty()) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Cart is empty");
        }
        // Chỉ chấp nhận số lượng dương và giá không âm trước khi cộng thành tiền.
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
            // Mã phải được cấp cho đúng người dùng và còn AVAILABLE; truy vấn hiện chưa kiểm tra hạn dùng.
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
            // Không giảm quá tiền hàng; số tiền được làm tròn đến hai chữ số thập phân.
            discountAmount = discountAmount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        }
        // Phí giao hàng hiện cố định 30.000, được cộng sau khi giảm giá tiền hàng.
        BigDecimal shippingFee = new BigDecimal("30000.00");
        BigDecimal total = subtotal.subtract(discountAmount).add(shippingFee);
        OrderSummaryResponse summary = new OrderSummaryResponse(subtotal, discountAmount, shippingFee, total);
        return new Checkout(items, assignment, summary);
    }

    private BigDecimal calculateLineTotal(CartItem item) {
        return item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Gói dữ liệu dùng chung giữa bước kiểm tra/tính tiền và bước ghi đơn.
    private record Checkout(List<CartItem> items, UserDiscount assignment, OrderSummaryResponse summary) {}
}
