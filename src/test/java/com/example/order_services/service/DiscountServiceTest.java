package com.example.order_services.service;

import com.example.order_services.common.DiscountType;
import com.example.order_services.entity.*;
import com.example.order_services.repository.*;
import com.example.order_services.service.impl.DiscountServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DiscountServiceTest {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesUsernameInServiceAndReturnsDiscountId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
        UserRepository users = mock(UserRepository.class);
        User user = User.builder().userName("alice").build();
        user.setId("alice-id");
        when(users.findByUserNameAndDeletedFalse("alice")).thenReturn(Optional.of(user));
        UserDiscountRepository assignments = mock(UserDiscountRepository.class);
        Discount discount = Discount.builder().discountType(DiscountType.PERCENTAGE).discountValue(new BigDecimal("10")).build();
        discount.setId("discount-id");
        when(assignments.findAvailableByUserId("alice-id"))
                .thenReturn(List.of(UserDiscount.builder().discount(discount).build()));
        var service = new DiscountServiceImpl(assignments, new CurrentUserService(users));

        var result = service.getDiscounts();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDiscountId()).isEqualTo("discount-id");
        assertThat(result.getFirst().getDiscountType()).isEqualTo("PERCENTAGE");
        assertThat(result.getFirst().getDiscountValue()).isEqualByComparingTo("10");
    }
}
