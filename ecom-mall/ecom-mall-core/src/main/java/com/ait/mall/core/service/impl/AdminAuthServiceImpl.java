package com.ait.mall.core.service.impl;

import com.ait.common.api.ResultCode;
import com.ait.common.constant.CommonConstants;
import com.ait.common.exception.BizException;
import com.ait.common.security.JwtTokenProvider;
import com.ait.mall.core.dto.AdminLoginRequest;
import com.ait.mall.core.dto.LoginResult;
import com.ait.mall.core.entity.SysAdmin;
import com.ait.mall.core.mapper.SysAdminMapper;
import com.ait.mall.core.service.AdminAuthService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final SysAdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Override
    public LoginResult login(AdminLoginRequest request) {
        SysAdmin admin = adminMapper.selectOne(Wrappers.<SysAdmin>lambdaQuery()
                .eq(SysAdmin::getUsername, request.getUsername()));
        if (admin == null) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (admin.getEnabled() != null && admin.getEnabled() == 0) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        String token = tokenProvider.createToken(admin.getId(),
                CommonConstants.AUDIENCE_ADMIN, CommonConstants.PLATFORM_WEB, admin.getUsername());

        SysAdmin patch = new SysAdmin();
        patch.setId(admin.getId());
        patch.setLastLoginAt(LocalDateTime.now());
        adminMapper.updateById(patch);

        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setExpiresIn(tokenProvider.getExpireSeconds());
        result.setUserId(admin.getId());
        result.setUsername(admin.getUsername());
        result.setNickname(admin.getNickname());
        result.setAvatar(admin.getAvatar());
        result.setRole(admin.getRole());
        return result;
    }

    @Override
    public SysAdmin profile(Long adminId) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        admin.setPassword(null);
        return admin;
    }

    @Override
    public SysAdmin updateProfile(Long adminId, SysAdmin request) {
        SysAdmin patch = new SysAdmin();
        patch.setId(adminId);
        patch.setNickname(request.getNickname());
        patch.setAvatar(request.getAvatar());
        patch.setPhone(request.getPhone());
        patch.setEmail(request.getEmail());
        adminMapper.updateById(patch);
        return profile(adminId);
    }

    @Override
    public void changePassword(Long adminId, String oldPassword, String newPassword) {
        SysAdmin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPassword, admin.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR, "原密码不正确");
        }
        SysAdmin patch = new SysAdmin();
        patch.setId(adminId);
        patch.setPassword(passwordEncoder.encode(newPassword));
        adminMapper.updateById(patch);
    }
}
