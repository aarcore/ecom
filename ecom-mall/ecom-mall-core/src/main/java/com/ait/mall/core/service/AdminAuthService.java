package com.ait.mall.core.service;

import com.ait.mall.core.dto.AdminLoginRequest;
import com.ait.mall.core.dto.LoginResult;
import com.ait.mall.core.entity.SysAdmin;

/**
 * Back-office operator authentication.
 */
public interface AdminAuthService {

    LoginResult login(AdminLoginRequest request);

    SysAdmin profile(Long adminId);

    SysAdmin updateProfile(Long adminId, SysAdmin request);

    void changePassword(Long adminId, String oldPassword, String newPassword);
}
