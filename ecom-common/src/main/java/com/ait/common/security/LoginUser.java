package com.ait.common.security;

import lombok.Data;

import java.io.Serializable;

/**
 * Authenticated principal resolved from the JWT on each request.
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    /** {@code admin} or {@code app}. */
    private String audience;

    /** {@code web} or {@code mp}. */
    private String platform;

    /** Raw token, kept so that logout can blacklist it. */
    private String token;

    /** Admin only: role code such as {@code ROLE_ADMIN}. */
    private String role;
}
