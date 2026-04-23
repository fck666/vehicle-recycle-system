package com.scrap_system.backend_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.session.redis", name = "enabled", havingValue = "true")
@Slf4j
public class RedisSessionCache implements SessionCache {

    private final StringRedisTemplate redisTemplate;
    private final AtomicBoolean unavailable = new AtomicBoolean(false);

    @Override
    public void put(String key, String value, long ttlSeconds) {
        execute(() -> redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS));
    }

    @Override
    public Optional<String> get(String key) {
        return execute(() -> Optional.ofNullable(redisTemplate.opsForValue().get(key)), Optional.empty());
    }

    @Override
    public void delete(String key) {
        execute(() -> redisTemplate.delete(key));
    }

    private void execute(Runnable action) {
        try {
            action.run();
            markAvailable();
        } catch (Exception ex) {
            markUnavailable(ex);
        }
    }

    private <T> T execute(ThrowingSupplier<T> action, T fallback) {
        try {
            T result = action.get();
            markAvailable();
            return result;
        } catch (Exception ex) {
            markUnavailable(ex);
            return fallback;
        }
    }

    private void markAvailable() {
        if (unavailable.compareAndSet(true, false)) {
            log.info("Redis session cache has recovered.");
        }
    }

    private void markUnavailable(Exception ex) {
        if (unavailable.compareAndSet(false, true)) {
            log.warn("Redis session cache is unavailable, falling back to DB only. cause={}({})",
                    ex.getClass().getSimpleName(), rootMessage(ex));
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? "unknown" : current.getMessage();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }
}
