package com.ait.mall.core.service;

import com.ait.common.api.PageResult;
import com.ait.mall.core.dto.AppLoginRequest;
import com.ait.mall.core.dto.LoginResult;
import com.ait.mall.core.dto.MpLoginRequest;
import com.ait.mall.core.vo.MemberVo;

/**
 * Customer account service: login for both storefront variants plus profile
 * maintenance.
 */
public interface MemberService {

    LoginResult loginByMobile(AppLoginRequest request);

    LoginResult loginByWechat(MpLoginRequest request);

    MemberVo profile(Long memberId);

    MemberVo updateProfile(Long memberId, MemberVo request);

    void changePassword(Long memberId, String oldPassword, String newPassword);

    /** Admin listing. */
    PageResult<MemberVo> pageForAdmin(String keyword, Integer status, String platform, int page, int limit);

    void setStatus(Long memberId, int status);

    /** Emits an SMS code; in demo mode the code is returned to the caller. */
    String sendSmsCode(String mobile);

    MemberVo detailForAdmin(Long memberId);
}
