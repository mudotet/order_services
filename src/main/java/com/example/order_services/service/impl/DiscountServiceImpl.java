package com.example.order_services.service.impl;

import com.example.order_services.dto.response.DiscountResponse;
import com.example.order_services.repository.DiscountRepository;
import com.example.order_services.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {
    private final DiscountRepository discountRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<DiscountResponse> getDiscounts() {
        return discountRepository.findAllByDeletedFalse()
                .stream()
                .map(discount -> modelMapper.map(discount, DiscountResponse.class))
                .toList();
    }
}
