package com.example.order_services.service;

import com.example.order_services.common.Role;
import com.example.order_services.dto.request.LoginRequest;
import com.example.order_services.dto.response.LoginResponse;
import com.example.order_services.entity.User;
import com.example.order_services.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    // Kiểm tra email, mật khẩu và vai trò rồi lưu phiên
    public LoginResponse login(LoginRequest login) {
        if (login.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid credentials");
        }
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(login.getEmail(), login.getPassword()));
        List<Role> roles = authentication.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.getAuthority().substring("ROLE_".length())))
                .toList();
        if (roles.isEmpty()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        // find User in db
        User user = userRepository.findByEmailAndDeletedFalse(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return LoginResponse.builder().userId(user.getId()).userName(user.getUserName())
                .email(user.getEmail()).roles(roles).build();
    }
}
