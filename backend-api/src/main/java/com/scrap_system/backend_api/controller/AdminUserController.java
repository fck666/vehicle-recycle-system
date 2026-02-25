package com.scrap_system.backend_api.controller;

import com.scrap_system.backend_api.dto.*;
import com.scrap_system.backend_api.model.Role;
import com.scrap_system.backend_api.model.UserAccount;
import com.scrap_system.backend_api.model.UserRole;
import com.scrap_system.backend_api.repository.RoleRepository;
import com.scrap_system.backend_api.repository.UserAccountRepository;
import com.scrap_system.backend_api.repository.UserRoleRepository;
import com.scrap_system.backend_api.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<Page<AdminUserDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        String query = isBlank(q) ? null : q.trim();
        Page<UserAccount> p = userAccountRepository.search(query, pageable);
        Map<Long, List<String>> roleMap = loadRoleMap(p.getContent());
        List<AdminUserDto> dtos = p.getContent().stream().map(u -> toDto(u, roleMap.get(u.getId()))).toList();
        return ResponseEntity.ok(new PageImpl<>(dtos, pageable, p.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDto> get(@PathVariable Long id) {
        return userAccountRepository.findById(id)
                .map(u -> ResponseEntity.ok(toDto(u, userRoleRepository.findRoleCodesByUserId(u.getId()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<AdminUserDto> create(@RequestBody AdminUserCreateRequest request) {
        if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().build();
        }
        try {
            UserAccount u = new UserAccount();
            u.setUsername(trimOrNull(request.getUsername()));
            u.setPhone(trimOrNull(request.getPhone()));
            u.setStatus(isBlank(request.getStatus()) ? "ACTIVE" : request.getStatus().trim().toUpperCase());
            u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            UserAccount saved = userAccountRepository.save(u);

            List<String> roles = normalizeRoles(request.getRoles());
            applyRoles(saved.getId(), roles, null);
            return ResponseEntity.ok(toDto(saved, userRoleRepository.findRoleCodesByUserId(saved.getId())));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<AdminUserDto> update(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request, Authentication authentication) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<UserAccount> existing = userAccountRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Long currentUserId = currentUserId(authentication);
        if (currentUserId != null && currentUserId.equals(id) && !isBlank(request.getStatus())) {
            String status = request.getStatus().trim().toUpperCase();
            if (!"ACTIVE".equals(status)) {
                return ResponseEntity.status(403).build();
            }
        }
        try {
            UserAccount u = existing.get();
            if (!isBlank(request.getUsername())) u.setUsername(trimOrNull(request.getUsername()));
            if (request.getPhone() != null) u.setPhone(trimOrNull(request.getPhone()));
            if (!isBlank(request.getStatus())) u.setStatus(request.getStatus().trim().toUpperCase());
            UserAccount saved = userAccountRepository.save(u);
            return ResponseEntity.ok(toDto(saved, userRoleRepository.findRoleCodesByUserId(saved.getId())));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/roles")
    @Transactional
    public ResponseEntity<AdminUserDto> updateRoles(@PathVariable Long id, @RequestBody AdminUserRolesUpdateRequest request, Authentication authentication) {
        Optional<UserAccount> existing = userAccountRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<String> newRoles = request == null ? List.of() : request.getRoles();
        List<String> roles = normalizeRoles(newRoles);

        Long currentUserId = currentUserId(authentication);
        if (currentUserId != null && currentUserId.equals(id) && !roles.contains("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        List<String> oldRoles = userRoleRepository.findRoleCodesByUserId(id);
        boolean wasAdmin = oldRoles.contains("ADMIN");
        boolean willAdmin = roles.contains("ADMIN");
        if (wasAdmin && !willAdmin && wouldRemoveLastAdmin(id)) {
            return ResponseEntity.status(409).build();
        }

        try {
            applyRoles(id, roles, authentication);
            UserAccount u = existing.get();
            return ResponseEntity.ok(toDto(u, userRoleRepository.findRoleCodesByUserId(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/reset-password")
    @Transactional
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody AdminUserResetPasswordRequest request) {
        if (request == null || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest().build();
        }
        Optional<UserAccount> existing = userAccountRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        UserAccount u = existing.get();
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userAccountRepository.save(u);
        sessionService.revokeAll(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sessions/revoke")
    public ResponseEntity<Void> revokeSessions(@PathVariable Long id) {
        if (!userAccountRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        sessionService.revokeAll(id);
        return ResponseEntity.noContent().build();
    }

    private boolean wouldRemoveLastAdmin(Long targetUserId) {
        Long adminRoleId = roleRepository.findByCode("ADMIN").map(Role::getId).orElse(null);
        if (adminRoleId == null) return false;
        long admins = userRoleRepository.countDistinctUserIdByRoleId(adminRoleId);
        if (admins <= 1) {
            return true;
        }
        return false;
    }

    private void applyRoles(Long userId, List<String> roleCodes, Authentication authentication) {
        Long currentUserId = currentUserId(authentication);
        if (currentUserId != null && currentUserId.equals(userId) && !roleCodes.contains("ADMIN")) {
            throw new IllegalArgumentException("cannot self-demote");
        }
        if (roleCodes.isEmpty()) {
            roleCodes = List.of("USER");
        }

        List<Long> roleIds = new ArrayList<>();
        for (String code : roleCodes) {
            Role r = roleRepository.findByCode(code).orElse(null);
            if (r == null) {
                throw new IllegalArgumentException("unknown role");
            }
            roleIds.add(r.getId());
        }

        userRoleRepository.deleteByUserId(userId);
        for (Long rid : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            userRoleRepository.save(ur);
        }
    }

    private Map<Long, List<String>> loadRoleMap(List<UserAccount> users) {
        if (users == null || users.isEmpty()) return Map.of();
        List<Long> userIds = users.stream().map(UserAccount::getId).filter(Objects::nonNull).toList();
        if (userIds.isEmpty()) return Map.of();
        List<Object[]> rows = userRoleRepository.findRoleCodesByUserIds(userIds);
        Map<Long, List<String>> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Long uid = (Long) row[0];
            String code = (String) row[1];
            map.computeIfAbsent(uid, k -> new ArrayList<>()).add(code);
        }
        return map;
    }

    private static AdminUserDto toDto(UserAccount u, List<String> roles) {
        return new AdminUserDto(
                u.getId(),
                u.getUsername(),
                u.getPhone(),
                u.getWxOpenid(),
                u.getWxUnionid(),
                u.getStatus(),
                roles == null ? List.of() : roles,
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }

    private static List<String> normalizeRoles(List<String> roles) {
        if (roles == null) return List.of("USER");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String r : roles) {
            if (r == null) continue;
            String t = r.trim().toUpperCase();
            if (!t.isEmpty()) set.add(t);
        }
        if (set.isEmpty()) set.add("USER");
        return new ArrayList<>(set);
    }

    private static Long currentUserId(Authentication authentication) {
        if (authentication == null) return null;
        Object p = authentication.getPrincipal();
        if (p instanceof Long l) return l;
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
