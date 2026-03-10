package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class AuthBindRequest {
    private String username;
    private String password;
}
