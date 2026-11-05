package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "Login result")
public class LoginResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private long expiresIn;

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;

    private String role;

    @Schema(description = "True when the account was created by this very login (mini-program first visit)")
    private boolean newAccount;
}
