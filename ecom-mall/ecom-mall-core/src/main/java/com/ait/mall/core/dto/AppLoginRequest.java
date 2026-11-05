package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@Schema(description = "C-end login request: password or sms code")
public class AppLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    @Schema(description = "Password, required when no sms code is supplied")
    @Size(max = 64, message = "密码过长")
    private String password;

    @Schema(description = "SMS verification code, alternative to password")
    private String smsCode;

    @Schema(description = "web or mp")
    private String platform;

    @Schema(description = "When true and the mobile is unknown, an account is created")
    private boolean autoRegister = true;
}
