package com.scrap_system.backend_api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        // Allow iframe for HTML preview
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/admin/miit-cp-jobs/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/vehicles/lookup").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/admin/files/upload").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.POST, "/api/admin/vehicles/*/images").hasAnyRole("ADMIN", "OPERATOR")
                // Dangerous batch operations should be ADMIN only
                .requestMatchers(HttpMethod.DELETE, "/api/admin/vehicles/batch/**").hasRole("ADMIN")
                // Allow ordinary users to view vehicle list and details
                .requestMatchers(HttpMethod.GET, "/api/admin/vehicles/**").authenticated()
                .requestMatchers("/api/vehicle-specs/batch").authenticated()
                .requestMatchers("/api/vehicle-documents/batch").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/material-prices/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/material-templates/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/vehicles/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/material-prices/batch").hasAnyRole("SERVICE", "ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.POST, "/api/material-prices").hasAnyRole("SERVICE", "ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/material-prices/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.POST, "/api/material-templates/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/material-templates/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.GET, "/api/admin/users/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/vehicle-mappings/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/admin/vehicle-mappings/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/external-trims/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/admin/external-trims/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/files/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers("/api/admin/files/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "OPERATOR")
                .anyRequest().authenticated()
        );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}
