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
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/error").permitAll()
                // Allow MIIT crawler related endpoints without token
                .requestMatchers("/api/admin/miit-cp-jobs/**").permitAll()
                .requestMatchers("/api/admin/vehicles/lookup").permitAll()
                .requestMatchers("/api/admin/files/upload").permitAll()
                .requestMatchers("/api/vehicle-specs/batch").permitAll()
                .requestMatchers("/api/vehicle-documents/batch").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/material-prices/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/material-templates/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
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
