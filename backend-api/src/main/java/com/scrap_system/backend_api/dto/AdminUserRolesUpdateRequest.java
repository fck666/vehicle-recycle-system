package com.scrap_system.backend_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminUserRolesUpdateRequest {
    private List<String> roles;
}

