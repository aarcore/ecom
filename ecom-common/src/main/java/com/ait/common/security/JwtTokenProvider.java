package com.ait.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Stateless JWT issuing/parsing shared by both the admin and the C-end module.
 *
 * <p>Two audiences are used so a C-end token can never be replayed against the
 * back office:
 * <ul>
 *   <li>{@code admin} — back-office operators</li>
 *   <li>{@code app} — store customers (web H5 and WeChat mini program)</li>
 * </ul>
 */
@Slf4j
public class JwtTokenProvider {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_AUDIENCE = "aud";
    private static final String CLAIM_PLATFORM = "plat";
    private static final String CLAIM_NAME = "name";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "ecom.jwt.secret must be at least 32 bytes for HS256, current length=" + secretBytes.length);
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String createToken(Long userId, String audience, String platform, String name) {
        Map<String, Object> claims = new HashMap<>(8);
        claims.put(CLAIM_UID, userId);
        claims.put(CLAIM_AUDIENCE, audience);
        claims.put(CLAIM_PLATFORM, platform);
        claims.put(CLAIM_NAME, name);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getExpireSeconds() * 1000L);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuer(properties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @return parsed claims, or {@code null} when the token is invalid/expired.
     */
    public Claims parse(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .setAllowedClockSkewSeconds(30)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("invalid token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Validates the token and returns the login principal.
     */
    public LoginUser resolve(String token, String expectedAudience) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        String audience = claims.get(CLAIM_AUDIENCE, String.class);
        if (expectedAudience != null && !expectedAudience.equals(audience)) {
            log.debug("token audience mismatch: expected={} actual={}", expectedAudience, audience);
            return null;
        }
        Object uid = claims.get(CLAIM_UID);
        if (uid == null) {
            return null;
        }
        LoginUser user = new LoginUser();
        user.setUserId(Long.valueOf(uid.toString()));
        user.setUsername(claims.get(CLAIM_NAME, String.class));
        user.setAudience(audience);
        user.setPlatform(claims.get(CLAIM_PLATFORM, String.class));
        user.setToken(token);
        return user;
    }

    public long getExpireSeconds() {
        return properties.getExpireSeconds();
    }
}
