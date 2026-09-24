package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.LoginRequest;
import com.example.order_services.dto.response.LoginResponse;
import com.example.order_services.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    // Đăng nhập bằng email và mật khẩu, trả thông tin tài khoản cùng các vai trò từ database.
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest login) {
        return BaseResponse.success(authService.login(login));
    }
}

// lêu lêu chi