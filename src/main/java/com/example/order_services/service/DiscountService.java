package com.example.order_services.service;

import com.example.order_services.dto.response.DiscountResponse;
import java.util.List;

/** Look up discount codes for the user. */
public interface DiscountService {
    List<DiscountResponse> getDiscounts();
}
