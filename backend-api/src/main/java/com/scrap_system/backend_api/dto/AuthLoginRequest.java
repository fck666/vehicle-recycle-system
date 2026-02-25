package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class AuthLoginRequest {
    private String username;
    private String password;
    private String clientType;
}
