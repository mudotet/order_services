package com.example.order_services.service;

import com.example.order_services.entity.User;
import com.example.order_services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    /** Đọc danh tính từ SecurityContext và đối chiếu lại tài khoản chưa bị xóa trong database. */
    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // Anonymous có thể có Authentication, nên cần kiểm tra riêng thay vì chỉ kiểm tra null.
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return userRepository.findByUserNameAndDeletedFalse(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User no longer active"));
    }
}
