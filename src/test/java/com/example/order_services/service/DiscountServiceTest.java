package com.example.order_services.service;

import com.example.order_services.dto.response.DiscountResponse;
import com.example.order_services.entity.Discount;
import com.example.order_services.repository.DiscountRepository;
import com.example.order_services.service.impl.DiscountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {
    @Mock
    private DiscountRepository discountRepository;

    private DiscountServiceImpl discountService;

    @BeforeEach
    void setUp() {
        discountService = new DiscountServiceImpl(discountRepository, new ModelMapper());
    }

    @Test
    void getDiscountsReturnsActiveDiscountData() {
        Discount discount = new Discount();
        discount.setId("discount-1");
        discount.setPercentageDiscount(new BigDecimal("10.00"));
        when(discountRepository.findAll()).thenReturn(List.of(discount));

        List<DiscountResponse> response = discountService.getDiscounts();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getId()).isEqualTo("discount-1");
        assertThat(response.getFirst().getPercentageDiscount())
                .isEqualByComparingTo("10.00");
    }
}
