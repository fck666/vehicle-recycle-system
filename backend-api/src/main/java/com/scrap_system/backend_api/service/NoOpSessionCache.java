package com.scrap_system.backend_api.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.session.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpSessionCache implements SessionCache {

    @Override
    public void put(String key, String value, long ttlSeconds) {
        // Redis session cache is optional in this environment.
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.empty();
    }

    @Override
    public void delete(String key) {
        // Redis session cache is optional in this environment.
    }
}
