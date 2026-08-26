package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.response.DiscountResponse;
import com.example.order_services.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/discounts")
public class DiscountController {
    private final DiscountService discountService;

    @GetMapping
    public BaseResponse<List<DiscountResponse>> getDiscounts() {
        return BaseResponse.success(discountService.getDiscounts());
    }
}
