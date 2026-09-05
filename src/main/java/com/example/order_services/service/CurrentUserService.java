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

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return userRepository.findByUserNameAndDeletedFalse(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User no longer active"));
    }
}
