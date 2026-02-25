package com.scrap_system.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String username;
    private String phone;
    private String wxOpenid;
    private String wxUnionid;
    private String status;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

