package com.example.order_services.service;

import com.example.order_services.dto.response.DiscountResponse;
import java.util.List;

public interface DiscountService {
    List<DiscountResponse> getDiscounts();
}
