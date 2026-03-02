package com.scrap_system.backend_api.service;

import com.scrap_system.backend_api.dto.AuthLoginResponse;
import com.scrap_system.backend_api.dto.AuthMeResponse;
import com.scrap_system.backend_api.model.UserAccount;
import com.scrap_system.backend_api.model.UserRole;
import com.scrap_system.backend_api.repository.RoleRepository;
import com.scrap_system.backend_api.repository.UserAccountRepository;
import com.scrap_system.backend_api.repository.UserRoleRepository;
import com.scrap_system.backend_api.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SessionService sessionService;

    @Value("${app.security.wx.dev-accept-openid:false}")
    private boolean devAcceptOpenid;

    @Value("${app.security.phone.dev-accept-phone:false}")
    private boolean devAcceptPhone;

    @Transactional
    public AuthLoginResponse login(String username, String password, String clientType) {
        if (username == null || username.trim().isEmpty() || password == null) {
            throw new IllegalArgumentException("bad credentials");
        }
        UserAccount u = userAccountRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("bad credentials"));
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("user disabled");
        }
        if (u.getPasswordHash() == null || u.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("bad credentials");
        }
        if (!passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new IllegalArgumentException("bad credentials");
        }
        List<String> roles = userRoleRepository.findRoleCodesByUserId(u.getId());
        var s = sessionService.issue(u.getId(), normalizeClientType(clientType, "WEB"));
        String token = jwtTokenService.issue(u.getId(), roles, s.getClientType(), s.getSessionId(), s.getTokenId());
        return new AuthLoginResponse(token, u.getId(), u.getUsername(), roles);
    }

    @Transactional(readOnly = true)
    public AuthMeResponse me(Long userId) {
        UserAccount u = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        List<String> roles = userRoleRepository.findRoleCodesByUserId(u.getId());
        return new AuthMeResponse(u.getId(), u.getUsername(), roles);
    }

    @Transactional
    public AuthLoginResponse wxLogin(String code, String openid, String unionid, String clientType) {
        String wxOpenid = normalize(openid);
        String wxUnionid = normalize(unionid);
        if (wxOpenid == null) {
            if (devAcceptOpenid) {
                throw new IllegalArgumentException("openid required");
            }
            throw new UnsupportedOperationException("wx code exchange not implemented");
        }

        UserAccount u = (wxUnionid != null ? userAccountRepository.findByWxUnionid(wxUnionid) : userAccountRepository.findByWxOpenid(wxOpenid))
                .orElseGet(() -> {
                    UserAccount nu = new UserAccount();
                    nu.setWxOpenid(wxOpenid);
                    nu.setWxUnionid(wxUnionid);
                    nu.setStatus("ACTIVE");
                    return userAccountRepository.save(nu);
                });
        if (u.getWxOpenid() == null && wxOpenid != null) u.setWxOpenid(wxOpenid);
        if (u.getWxUnionid() == null && wxUnionid != null) u.setWxUnionid(wxUnionid);
        userAccountRepository.save(u);

        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("user disabled");
        }

        Long userRoleId = roleRepository.findByCode("USER").map(r -> r.getId()).orElse(null);
        if (userRoleId != null) {
            boolean has = userRoleRepository.findByUserId(u.getId()).stream().anyMatch(ur -> ur.getRoleId().equals(userRoleId));
            if (!has) {
                UserRole ur = new UserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(userRoleId);
                userRoleRepository.save(ur);
            }
        }

        List<String> roles = userRoleRepository.findRoleCodesByUserId(u.getId());
        var s = sessionService.issue(u.getId(), normalizeClientType(clientType, "MINIAPP"));
        String token = jwtTokenService.issue(u.getId(), roles, s.getClientType(), s.getSessionId(), s.getTokenId());
        return new AuthLoginResponse(token, u.getId(), u.getUsername(), roles);
    }

    @Transactional
    public AuthLoginResponse phoneLogin(String phone, String code, String clientType) {
        String p = normalize(phone);
        if (p == null) {
            throw new IllegalArgumentException("phone required");
        }
        if (!devAcceptPhone) {
            throw new UnsupportedOperationException("phone login not implemented");
        }
        if (normalize(code) == null) {
            throw new IllegalArgumentException("code required");
        }

        UserAccount u = userAccountRepository.findByPhone(p).orElseGet(() -> {
            UserAccount nu = new UserAccount();
            nu.setPhone(p);
            nu.setStatus("ACTIVE");
            return userAccountRepository.save(nu);
        });
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("user disabled");
        }

        Long userRoleId = roleRepository.findByCode("USER").map(r -> r.getId()).orElse(null);
        if (userRoleId != null) {
            boolean has = userRoleRepository.findByUserId(u.getId()).stream().anyMatch(ur -> ur.getRoleId().equals(userRoleId));
            if (!has) {
                UserRole ur = new UserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(userRoleId);
                userRoleRepository.save(ur);
            }
        }

        List<String> roles = userRoleRepository.findRoleCodesByUserId(u.getId());
        var s = sessionService.issue(u.getId(), normalizeClientType(clientType, "MINIAPP"));
        String token = jwtTokenService.issue(u.getId(), roles, s.getClientType(), s.getSessionId(), s.getTokenId());
        return new AuthLoginResponse(token, u.getId(), u.getUsername(), roles);
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeClientType(String clientType, String defaultValue) {
        String ct = normalize(clientType);
        if (ct == null) return defaultValue;
        return ct.toUpperCase();
    }
}
