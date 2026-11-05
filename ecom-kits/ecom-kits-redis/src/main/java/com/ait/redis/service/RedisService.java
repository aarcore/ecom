package com.ait.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Thin convenience facade over {@link StringRedisTemplate}.
 *
 * <p>Deliberately narrow: it exposes exactly what the platform uses today — the
 * SMS verification code and its send-rate limiter. Wrappers that merely forward
 * to the template add a layer to read through without changing semantics, so
 * anything not actually called from the codebase does not belong here.
 */
@Component
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
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
}
