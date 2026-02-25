package com.scrap_system.backend_api.config;

import com.scrap_system.backend_api.model.Role;
import com.scrap_system.backend_api.model.UserAccount;
import com.scrap_system.backend_api.model.UserRole;
import com.scrap_system.backend_api.repository.RoleRepository;
import com.scrap_system.backend_api.repository.UserAccountRepository;
import com.scrap_system.backend_api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdAdminInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureRoles(List.of("ADMIN", "OPERATOR", "USER", "SERVICE"));

        String rawUsername = System.getenv().getOrDefault("ADMIN_USERNAME", "fcc");
        String username = rawUsername == null || rawUsername.trim().isEmpty() ? "fcc" : rawUsername.trim();
        String pw = System.getenv("ADMIN_PASSWORD");

        UserAccount admin = userAccountRepository.findByUsername(username).orElse(null);
        if (admin == null) {
            if (pw == null || pw.trim().isEmpty()) {
                if (userAccountRepository.count() == 0) {
                    throw new IllegalStateException("ADMIN_PASSWORD is required in prod when user_account is empty");
                }
                log.warn("ADMIN_PASSWORD is blank; skip creating admin user: username={}", username);
                return;
            }
            UserAccount u = new UserAccount();
            u.setUsername(username);
            u.setStatus("ACTIVE");
            u.setPasswordHash(passwordEncoder.encode(pw.trim()));
            admin = userAccountRepository.save(u);
        }

        attachRole(admin.getId(), "ADMIN");
        log.info("prod admin user ensured: username={}", username);
    }

    private void ensureRoles(List<String> codes) {
        for (String code : codes) {
            roleRepository.findByCode(code).orElseGet(() -> {
                Role r = new Role();
                r.setCode(code);
                return roleRepository.save(r);
            });
        }
    }

    private void attachRole(Long userId, String roleCode) {
        Long roleId = roleRepository.findByCode(roleCode).map(Role::getId).orElse(null);
        if (roleId == null || userId == null) return;
        boolean has = userRoleRepository.findByUserId(userId).stream().anyMatch(ur -> ur.getRoleId().equals(roleId));
        if (!has) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleRepository.save(ur);
        }
    }
}
