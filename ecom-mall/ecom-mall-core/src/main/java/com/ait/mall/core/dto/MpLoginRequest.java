package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * WeChat mini-program silent login. The client exchanges {@code wx.login()}'s
 * code for a session; in demo mode the server accepts the code as-is and
 * derives a stable openid from it so the flow is testable without an AppID.
 */
@Data
@Schema(description = "Mini program login request")
public class MpLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "缺少微信登录凭证")
    @Schema(description = "code returned by wx.login()")
    private String code;

    @Schema(description = "Optional nickname from wx.getUserProfile()")
    private String nickname;

    @Schema(description = "Optional avatar url")
    private String avatar;
}
