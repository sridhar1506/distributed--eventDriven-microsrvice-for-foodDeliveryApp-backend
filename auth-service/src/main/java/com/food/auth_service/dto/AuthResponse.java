package com.food.auth_service.dto;

import com.food.auth_service.Entity.User.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {

    private String jwt;
    private String message;
    private UserRole role;
}
