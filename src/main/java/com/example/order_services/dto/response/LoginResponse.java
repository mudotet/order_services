package com.example.order_services.dto.response;

import com.example.order_services.common.Role;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String userId;
    private String userName;
    private String email;
    private List<Role> roles;
}
