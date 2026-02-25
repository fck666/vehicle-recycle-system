package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AuthLoginResponse {
    private String token;
    private Long userId;
    private String username;
    private List<String> roles;
}

