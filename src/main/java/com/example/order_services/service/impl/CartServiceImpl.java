package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.common.StockStatus;
import com.example.order_services.dto.request.AdjustCartItemQuantityRequest;
import com.example.order_services.dto.response.CartDetailResponse;
import com.example.order_services.dto.response.CartItemResponse;
import com.example.order_services.entity.*;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.*;
import com.example.order_services.service.CartService;
import com.example.order_services.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Đọc giỏ và điều chỉnh số lượng trong phạm vi người dùng đang đăng nhập. */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final CurrentUserService currentUserService;

    @Override
    public CartDetailResponse getCartDetail() {
        User user = currentUserService.getCurrentUser();
        Cart cart = cartRepository.findByUser_IdAndDeletedFalse(user.getId()).orElse(null);
        // Chưa có giỏ được biểu diễn bằng danh sách rỗng và tạm tính bằng 0.
        if (cart == null) {
            return new CartDetailResponse(null, List.of(), new BigDecimal("0.00"));
        }
        List<CartItem> items = cartItemRepository.findActiveItemsByCartId(cart.getId());
        // Tải tồn kho theo lô cho các biến thể trong giỏ, sau đó tra cứu khi dựng từng DTO.
        List<String> variantIds = items.stream().map(item -> item.getProductVariant().getId()).distinct().toList();
        Map<String, Inventory> inventories = variantIds.isEmpty() ? Map.of()
                : inventoryRepository.findAllByProductVariantIdInAndDeletedFalse(variantIds).stream()
                    .collect(Collectors.toMap(inventory -> inventory.getProductVariant().getId(), Function.identity()));
        List<CartItemResponse> responses = items.stream().map(item -> {
            ProductVariant variant = item.getProductVariant();
            Inventory inventory = inventories.get(variant.getId());
            if (inventory == null) {
                throw new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found");
            }
            return CartItemResponse.builder()
                    .cartItemId(item.getId())
                    .productVariantId(variant.getId())
                    .productName(buildProductName(variant))
                    .quantity(item.getProductQuantity())
                    .lineTotal(calculateLineTotal(item))
                    .stockStatus(calculateStockStatus(inventory.getQuantityInStock(), item.getProductQuantity()))
                    .build();
        }).toList();
        BigDecimal subtotal = responses.stream().map(CartItemResponse::getLineTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new CartDetailResponse(cart.getId(), responses, subtotal);
    }

    /** Mỗi thao tác chỉ tăng/giảm một đơn vị; giảm về 0 sẽ xóa mềm dòng giỏ. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Integer adjustCartItemQuantity(String cartItemId, AdjustCartItemQuantityRequest request) {
        Integer change = request == null ? null : request.getQuantityChange();
        if (change == null || (change != 1 && change != -1)) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Quantity change must be 1 or -1");
        }
        User user = currentUserService.getCurrentUser();
        // Khóa giỏ để tuần tự hóa thay đổi số lượng; luồng tạo đơn cũng yêu cầu khóa cùng giỏ.
        // Share the checkout lock so button clicks cannot race each other or order creation.
        Cart cart = cartRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Cart item not found"));
        List<CartItem> items = cartItemRepository.findActiveItemsByCartId(cart.getId());
        // Tìm trong giỏ của người hiện tại để không sửa được cartItemId thuộc người khác.
        CartItem item = items.stream().filter(candidate -> candidate.getId().equals(cartItemId)).findFirst()
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Cart item not found"));
        if (item.getProductQuantity() == null || item.getProductQuantity() <= 0) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid cart item quantity");
        }
        if (change == 1) {
            String variantId = item.getProductVariant().getId();
            Inventory inventory = inventoryRepository.findByProductVariantIdsForUpdate(List.of(variantId)).stream()
                    .findFirst().orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found"));
            // Cộng tất cả dòng cùng biến thể và đơn vị sắp tăng để tránh vượt kho khi có dòng trùng.
            long requestedQuantity = change;
            for (CartItem candidate : items) {
                if (candidate.getProductVariant().getId().equals(variantId)) {
                    if (candidate.getProductQuantity() == null || candidate.getProductQuantity() <= 0) {
                        throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid cart item quantity");
                    }
                    requestedQuantity += candidate.getProductQuantity();
                }
            }
            if (requestedQuantity > inventory.getQuantityInStock()) {
                throw new ApplicationException(EnumCode.BAD_REQUEST, "Insufficient stock");
            }
        }
        int newQuantity = item.getProductQuantity() + change;
        item.setProductQuantity(newQuantity);
        if (newQuantity == 0) {
            item.setDeleted(true);
        }
        cartItemRepository.save(item);
        return newQuantity;
    }

    private BigDecimal calculateLineTotal(CartItem item) {
        return item.getProductVariant().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildProductName(ProductVariant variant) {
        return variant.getProduct().getProductName() + " " + variant.getProductVariant();
    }

    // Trạng thái phản ánh tồn còn lại sau số lượng đang chọn; dưới 10 là LIMITED_STOCK.
    private String calculateStockStatus(int stock, int quantity) {
        if (stock <= 0 || stock < quantity) {
            return StockStatus.OUT_OF_STOCK.name();
        }
        int remaining = stock - quantity;
        return remaining < 10 ? StockStatus.LIMITED_STOCK.name() + " " + remaining : StockStatus.IN_STOCK.name();
    }
}
