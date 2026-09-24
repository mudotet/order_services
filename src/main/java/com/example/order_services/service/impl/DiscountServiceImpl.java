package com.example.order_services.service.impl;

import com.example.order_services.dto.response.DiscountResponse;
import com.example.order_services.repository.UserDiscountRepository;
import com.example.order_services.service.CurrentUserService;
import com.example.order_services.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Provide available discount codes for the signed-in user. */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@Transactional(readOnly = true)
public class DiscountServiceImpl implements DiscountService {
    private final UserDiscountRepository userDiscountRepository;
    private final CurrentUserService currentUserService;

    /** Fetch unused codes assigned to the current user with AVAILABLE status; expiration dates are not yet checked. */
    @Override
    public List<DiscountResponse> getDiscounts() {
        String userId = currentUserService.getCurrentUser().getId();
        return userDiscountRepository.findAvailableByUserId(userId).stream()
                .map(assignment -> {
                    var discount = assignment.getDiscount();
                    return new DiscountResponse(discount.getId(), discount.getDiscountType().name(), discount.getDiscountValue());
                }).toList();
    }
}
