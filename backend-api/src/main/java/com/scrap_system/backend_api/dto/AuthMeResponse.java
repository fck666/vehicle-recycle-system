package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AuthMeResponse {
    private Long userId;
    private String username;
    private List<String> roles;
}

