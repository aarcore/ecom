package com.ait.mall.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
@Schema(description = "Back-office login request")
public class AdminLoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入账号")
    @Size(max = 64, message = "账号过长")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(max = 64, message = "密码过长")
    private String password;
}
