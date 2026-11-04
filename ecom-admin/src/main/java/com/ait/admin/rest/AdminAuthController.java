package com.ait.admin.rest;

import com.ait.common.api.R;
import com.ait.common.security.LoginUserContext;
import com.ait.mall.core.dto.AdminLoginRequest;
import com.ait.mall.core.dto.LoginResult;
import com.ait.mall.core.entity.SysAdmin;
import com.ait.mall.core.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin · 认证")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(summary = "后台账号登录")
    public R<LoginResult> login(@Valid @RequestBody AdminLoginRequest request) {
        return R.ok(adminAuthService.login(request));
    }

    @GetMapping("/profile")
    @Operation(summary = "当前登录运营信息")
    public R<SysAdmin> profile() {
        return R.ok(adminAuthService.profile(LoginUserContext.requiredUserId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新运营资料")
    public R<SysAdmin> updateProfile(@RequestBody SysAdmin request) {
        return R.ok(adminAuthService.updateProfile(LoginUserContext.requiredUserId(), request));
    }

    @PostMapping("/password")
    @Operation(summary = "修改登录密码")
    public R<Void> changePassword(@RequestBody PasswordRequest request) {
        adminAuthService.changePassword(LoginUserContext.requiredUserId(),
                request.getOldPassword(), request.getNewPassword());
        return R.ok();
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout() {
        // stateless JWT: the client drops the token. The endpoint exists so the
        // console can call it symmetrically and future blacklisting has a hook.
        LoginUserContext.clear();
        return R.ok();
    }

    @lombok.Data
    public static class PasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
