package com.example.order_services.config;

import com.example.order_services.common.Role;
import com.example.order_services.repository.UserRepository;
import com.example.order_services.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;


    // Spring requires the method name loadUserByUsername, but this application looks up accounts by email only.
    @Override
    public UserDetails loadUserByUsername(String email) {
        com.example.order_services.entity.User user = userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<String> roles = userRoleRepository.findAllByUser_IdAndDeletedFalseAndRole_DeletedFalse(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getRoleName())
                .collect(Collectors.toSet());

        List<GrantedAuthority> authorities = Arrays.stream(Role.values())
                .filter(role -> roles.contains(role.name()))
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();

        return new User(user.getEmail(), user.getPassword(), authorities);
    }
}
