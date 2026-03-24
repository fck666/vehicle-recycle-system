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
    public void updateUsername(Long userId, String newUsername) {
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("username cannot be empty");
        }
        String normalizedUsername = newUsername.trim();
        UserAccount u = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        
        // 检查用户名是否已存在（排除自己）
        Optional<UserAccount> existing = userAccountRepository.findByUsername(normalizedUsername);
        if (existing.isPresent() && !existing.get().getId().equals(userId)) {
            throw new IllegalArgumentException("username already exists");
        }
        
        u.setUsername(normalizedUsername);
        userAccountRepository.save(u);
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
                    // 新增：使用 openid 后 6 位作为默认用户名，方便后台区分
                    String suffix = finalWxOpenid.length() > 6 ? finalWxOpenid.substring(finalWxOpenid.length() - 6) : finalWxOpenid;
                    nu.setUsername("wx_" + suffix);
                    return userAccountRepository.save(nu);
                });
        if (u.getWxOpenid() == null && wxOpenid != null) u.setWxOpenid(wxOpenid);
        if (u.getWxUnionid() == null && wxUnionid != null) u.setWxUnionid(wxUnionid);
        // 如果之前注册时没有生成用户名，在这里补上
        if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
            String suffix = wxOpenid.length() > 6 ? wxOpenid.substring(wxOpenid.length() - 6) : wxOpenid;
            u.setUsername("wx_" + suffix);
        }
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
        
        // 只能用临时的微信账号去绑定，如果当前账号已经是正式员工账号，不允许再次绑定并防止被误删
        if (wxUser.getUsername() != null && !wxUser.getUsername().startsWith("wx_")) {
            throw new IllegalArgumentException("当前微信已绑定过员工账号，请勿重复绑定");
        }

        // 查找要绑定的后台账号
        UserAccount staffUser = userAccountRepository.findByUsername(username != null ? username.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("找不到该员工账号"));

        if (password == null || staffUser.getPasswordHash() == null || !passwordEncoder.matches(password, staffUser.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 检查该后台账号是否已经被绑定过
        if (staffUser.getWxOpenid() != null && !staffUser.getWxOpenid().equals(wxUser.getWxOpenid())) {
            throw new IllegalArgumentException("该员工账号已被其他微信绑定");
        }

        // 核心逻辑：将微信信息从临时账号转移到后台账号
        String openid = wxUser.getWxOpenid();
        String unionid = wxUser.getWxUnionid();

        // 1. 先把临时账号的微信信息清空并保存（腾出唯一索引位置）
        wxUser.setWxOpenid(null);
        wxUser.setWxUnionid(null);
        userAccountRepository.saveAndFlush(wxUser);

        // 2. 将微信信息绑定到后台账号
        staffUser.setWxOpenid(openid);
        staffUser.setWxUnionid(unionid);
        userAccountRepository.saveAndFlush(staffUser);

        sessionService.revokeAll(staffUser.getId());
        sessionService.revokeAll(wxUser.getId());

        // 3. 删除临时的微信账号
        if (!staffUser.getId().equals(wxUser.getId())) {
            // 删除与临时微信账号关联的用户角色关系
            userRoleRepository.deleteAllByUserId(wxUser.getId());
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
