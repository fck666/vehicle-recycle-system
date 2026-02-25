package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class AdminUserUpdateRequest {
    private String username;
    private String phone;
    private String status;
}

