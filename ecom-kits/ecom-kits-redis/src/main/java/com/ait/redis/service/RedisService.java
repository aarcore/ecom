package com.ait.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Thin convenience facade over {@link StringRedisTemplate} covering the handful
 * of patterns this platform actually needs (sms codes, token blacklist, stock
 * locks, distributed locks).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public long increment(String key, long delta, Duration ttl) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        if (value != null && value == delta) {
            redisTemplate.expire(key, ttl);
        }
        return value == null ? 0 : value;
    }

    /**
     * Caches a value produced by {@code loader}. Never throws on a redis
     * outage: it degrades to calling the loader directly.
     */
    public String remember(String key, Duration ttl, Supplier<String> loader) {
        try {
            String cached = get(key);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            log.warn("redis read failed for {}: {}", key, ex.getMessage());
            return loader.get();
        }

        String value = loader.get();
        if (value != null) {
            try {
                set(key, value, ttl);
            } catch (Exception ex) {
                log.warn("redis write failed for {}: {}", key, ex.getMessage());
            }
        }
        return value;
    }

    /**
     * Best-effort non-blocking lock used to serialise stock deduction.
     */
    public boolean tryLock(String key, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl.toMillis(), TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(ok);
    }

    public void unlock(String key) {
        try {
            delete(key);
        } catch (Exception ex) {
            log.warn("failed to release lock {}: {}", key, ex.getMessage());
        }
    }
}
