package com.example.order_services.service;

import com.example.order_services.dto.response.DiscountResponse;
import java.util.List;

/** Tra cứu mã giảm giá của người dùng. */
public interface DiscountService {
    List<DiscountResponse> getDiscounts();
}
