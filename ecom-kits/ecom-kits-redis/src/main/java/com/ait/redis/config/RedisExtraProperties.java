package com.ait.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Platform specific redis switches, kept separate from the standard Spring
 * {@code spring.redis.*} block.
 */
@Data
@ConfigurationProperties(prefix = "ecom.redis")
public class RedisExtraProperties {

    /** Key namespace so several environments can share one redis instance. */
    private String namespace = "ecom";

    /** Default TTL in seconds for cached catalogue data. */
    private long cacheTtlSeconds = 300;

    public String key(String raw) {
        return namespace + ":" + raw;
    }
}
