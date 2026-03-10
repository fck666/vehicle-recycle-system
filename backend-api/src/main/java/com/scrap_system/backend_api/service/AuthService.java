package com.scrap_system.backend_api.service;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.scrap_system.backend_api.dto.AuthLoginResponse;
import com.scrap_system.backend_api.dto.AuthMeResponse;
import com.scrap_system.backend_api.model.Role;
import com.scrap_system.backend_api.model.UserAccount;
import com.scrap_system.backend_api.model.UserRole;
import com.scrap_system.backend_api.repository.RoleRepository;
import com.scrap_system.backend_api.repository.UserAccountRepository;
import com.scrap_system.backend_api.repository.UserRoleRepository;
import com.scrap_system.backend_api.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SessionService sessionService;
    private final WxMaService wxMaService;

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

        // 如果 code 不为空，先尝试通过微信换取 openid
        if (code != null && !code.trim().isEmpty()) {
            try {
                WxMaJscode2SessionResult session = wxMaService.getUserService().getSessionInfo(code);
                wxOpenid = session.getOpenid();
                wxUnionid = session.getUnionid();
            } catch (WxErrorException e) {
                throw new RuntimeException("WeChat login failed: " + e.getError().getErrorMsg());
            }
        }

        if (wxOpenid == null) {
            if (devAcceptOpenid) {
                throw new IllegalArgumentException("openid required");
            }
            throw new IllegalArgumentException("wx code exchange failed, openid is null");
        }

        String finalWxOpenid = wxOpenid;
        String finalWxUnionid = wxUnionid;
        UserAccount u = (wxUnionid != null ? userAccountRepository.findByWxUnionid(wxUnionid) : userAccountRepository.findByWxOpenid(wxOpenid))
                .orElseGet(() -> {
                    UserAccount nu = new UserAccount();
                    nu.setWxOpenid(finalWxOpenid);
                    nu.setWxUnionid(finalWxUnionid);
                    nu.setStatus("ACTIVE");
                    return userAccountRepository.save(nu);
                });
        if (u.getWxOpenid() == null && wxOpenid != null) u.setWxOpenid(wxOpenid);
        if (u.getWxUnionid() == null && wxUnionid != null) u.setWxUnionid(wxUnionid);
        userAccountRepository.save(u);

        if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("user disabled");
        }

        Long userRoleId = roleRepository.findByCode("USER").map(Role::getId).orElse(null);
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
    public void bindStaff(Long userId, String username, String password) {
        UserAccount wxUser = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        
        // 查找要绑定的后台账号
        UserAccount staffUser = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("staff account not found"));

        if (!passwordEncoder.matches(password, staffUser.getPasswordHash())) {
            throw new IllegalArgumentException("invalid password");
        }

        // 检查该后台账号是否已经被绑定过
        if (staffUser.getWxOpenid() != null && !staffUser.getWxOpenid().equals(wxUser.getWxOpenid())) {
            throw new IllegalArgumentException("this staff account is already bound to another wechat");
        }

        // 核心逻辑：将微信信息合并到后台账号上，并删除临时的微信账号
        staffUser.setWxOpenid(wxUser.getWxOpenid());
        staffUser.setWxUnionid(wxUser.getWxUnionid());
        userAccountRepository.save(staffUser);

        // 如果当前的微信账号和后台账号不是同一个，删除临时的微信账号
        if (!staffUser.getId().equals(wxUser.getId())) {
            // 注意：这里可能需要迁移微信账号关联的其它数据（如估值历史），暂时先简单处理
            userAccountRepository.delete(wxUser);
        }
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
