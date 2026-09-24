package com.example.order_services.service;

import com.example.order_services.entity.User;
import com.example.order_services.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    /** Read the email from SecurityContext and verify that the account still exists and is not deleted in the database. */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SHIPPER')")
    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        // Anonymous users may have an Authentication object, so check for them explicitly instead of only checking for null.
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return userRepository.findByEmailAndDeletedFalse(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User no longer active"));
    }
}
