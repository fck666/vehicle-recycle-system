package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserCreateRequest {
    private String username;
    private String password;
    private String phone;
    private String status;
    private List<String> roles;
}

