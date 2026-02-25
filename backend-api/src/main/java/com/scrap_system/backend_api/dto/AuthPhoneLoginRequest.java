package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class AuthPhoneLoginRequest {
    private String phone;
    private String code;
    private String clientType;
}

