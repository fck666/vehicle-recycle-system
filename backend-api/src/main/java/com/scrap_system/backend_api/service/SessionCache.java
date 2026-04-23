package com.scrap_system.backend_api.service;

import java.util.Optional;

public interface SessionCache {

    void put(String key, String value, long ttlSeconds);

    Optional<String> get(String key);

    void delete(String key);
}
