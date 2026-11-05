package com.ait.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for JWT issuing and for the whitelists that the module
 * specific interceptors use.
 */
@Data
@ConfigurationProperties(prefix = "ecom.jwt")
public class JwtProperties {

    /** HMAC-SHA256 signing secret. Must be at least 32 characters. */
    private String secret = "ecom-platform-default-jwt-secret-please-change-me";

    /** Access token TTL in seconds (default 7 days). */
    private long expireSeconds = 7 * 24 * 3600L;

    /** Issuer recorded in the token. */
    private String issuer = "ecom";

    /** Paths that never require a token for the admin module. */
    private List<String> adminWhitelist = new ArrayList<>();

    /** Paths that never require a token for the C-end module. */
    private List<String> appWhitelist = new ArrayList<>();
}
