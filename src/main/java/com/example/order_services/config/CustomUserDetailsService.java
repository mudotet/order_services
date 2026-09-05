package com.example.order_services.config;

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


import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;


    @Override
    public UserDetails loadUserByUsername(String username) {
        com.example.order_services.entity.User user = userRepository.findByUserNameAndDeletedFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<GrantedAuthority> authorities = userRoleRepository.findAllByUser_IdAndDeletedFalseAndRole_DeletedFalse(user.getId())
                .stream()
                .<GrantedAuthority>map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getRoleName()))
                .toList();

        return new User(user.getUserName(), user.getPassword(), authorities);
    }
}
