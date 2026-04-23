package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.AuthBindRequest;
import com.scrap_system.backend_api.dto.AuthLoginRequest;
import com.scrap_system.backend_api.dto.AuthLoginResponse;
import com.scrap_system.backend_api.dto.AuthMeResponse;
import com.scrap_system.backend_api.dto.AuthPhoneLoginRequest;
import com.scrap_system.backend_api.dto.AuthWxLoginRequest;
import com.scrap_system.backend_api.dto.UpdateUsernameRequest;
import com.scrap_system.backend_api.exception.ContentSecurityException;
import com.scrap_system.backend_api.security.JwtTokenService;
import com.scrap_system.backend_api.service.AuthService;
import com.scrap_system.backend_api.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;
    private final SessionService sessionService;

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(authService.login(request.getUsername(), request.getPassword(), request.getClientType()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthMeResponse> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(authService.me(userId));
    }

    @PutMapping("/me/username")
    public ResponseEntity<?> updateUsername(@RequestBody UpdateUsernameRequest request, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            authService.updateUsername(userId, request.getUsername());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException | ContentSecurityException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        try {
            JwtTokenService.ParsedToken parsed = jwtTokenService.parse(token);
            sessionService.revoke(parsed.userId(), parsed.clientType());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/wx/login")
    public ResponseEntity<AuthLoginResponse> wxLogin(@RequestBody AuthWxLoginRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(authService.wxLogin(request.getCode(), request.getOpenid(), request.getUnionid(), request.getClientType()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(null);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/wx/bind")
    public ResponseEntity<?> wxBind(@RequestBody AuthBindRequest request, Authentication authentication) {
        if (request == null || authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = (Long) authentication.getPrincipal();
        try {
            authService.bindStaff(userId, request.getUsername(), request.getPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/phone/login")
    public ResponseEntity<AuthLoginResponse> phoneLogin(@RequestBody AuthPhoneLoginRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(authService.phoneLogin(request.getPhone(), request.getCode(), request.getClientType()));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(501).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).build();
        }
    }
}
