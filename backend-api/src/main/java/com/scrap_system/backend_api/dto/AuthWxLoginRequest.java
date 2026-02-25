package com.scrap_system.backend_api.dto;

import lombok.Data;

@Data
public class AuthWxLoginRequest {
    private String code;
    private String openid;
    private String unionid;
    private String clientType;
}
