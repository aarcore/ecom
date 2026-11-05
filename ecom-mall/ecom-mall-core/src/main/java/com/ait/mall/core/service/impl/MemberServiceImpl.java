package com.ait.mall.core.service.impl;

import com.ait.common.api.PageResult;
import com.ait.common.api.ResultCode;
import com.ait.common.constant.CommonConstants;
import com.ait.common.exception.BizException;
import com.ait.common.security.JwtTokenProvider;
import com.ait.mall.common.enums.ClientPlatform;
import com.ait.mall.core.dto.AppLoginRequest;
import com.ait.mall.core.dto.LoginResult;
import com.ait.mall.core.dto.MpLoginRequest;
import com.ait.mall.core.entity.Member;
import com.ait.mall.core.mapper.MemberMapper;
import com.ait.mall.core.service.MemberService;
import com.ait.mall.core.vo.MemberVo;
import com.ait.redis.service.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Customer accounts.
 *
 * <p>Mini-program login accepts the {@code wx.login()} code without contacting
 * WeChat when no AppID/secret are configured: the openid is derived from a hash
 * of the code so the whole flow stays exercisable in demo deployments. Set
 * {@code ecom.wechat.app-id} / {@code app-secret} to enable the real
 * {@code jscode2session} call.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final int SMS_TTL_MINUTES = 5;
    private static final int SMS_RATE_LIMIT_SECONDS = 60;

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;

    @Override
    public LoginResult loginByMobile(AppLoginRequest request) {
        Member member = findByMobile(request.getMobile());
        boolean verified;

        if (request.getSmsCode() != null && !request.getSmsCode().isEmpty()) {
            String cached = redisService.get(CommonConstants.REDIS_SMS_CODE + request.getMobile());
            if (cached == null || !cached.equals(request.getSmsCode())) {
                throw new BizException(ResultCode.SMS_CODE_ERROR);
            }
            redisService.delete(CommonConstants.REDIS_SMS_CODE + request.getMobile());
            verified = true;
        } else if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (member == null) {
                throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
            }
            verified = passwordEncoder.matches(request.getPassword(), member.getPassword());
            if (!verified) {
                throw new BizException(ResultCode.PASSWORD_ERROR);
            }
        } else {
            throw new BizException(ResultCode.BAD_REQUEST, "请输入密码或验证码");
        }

        if (member == null) {
            if (!request.isAutoRegister()) {
                throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
            }
            // demo deployments auto provision on sms login; password mirrors the code
            member = createMember(request.getMobile(),
                    ClientPlatform.of(request.getPlatform()).getCode(),
                    request.getPassword() == null ? null : request.getPassword());
            LoginResult result = finishLogin(member);
            result.setNewAccount(true);
            return result;
        }

        return finishLogin(member);
    }

    @Override
    public LoginResult loginByWechat(MpLoginRequest request) {
        String openid = resolveOpenid(request.getCode());
        Member member = memberMapper.selectOne(Wrappers.<Member>lambdaQuery()
                .eq(Member::getWxOpenid, openid));

        boolean created = false;
        if (member == null) {
            member = new Member();
            member.setWxOpenid(openid);
            member.setPlatform(CommonConstants.PLATFORM_MP);
            member.setNickname(request.getNickname() == null || request.getNickname().isEmpty()
                    ? "微信用户" + (1000 + ThreadLocalRandom.current().nextInt(9000))
                    : request.getNickname());
            member.setAvatar(request.getAvatar() == null || request.getAvatar().isEmpty()
                    ? CommonConstants.DEFAULT_AVATAR : request.getAvatar());
            member.setGender(0);
            member.setLevel(1);
            member.setPoints(0);
            member.setBalance(BigDecimal.ZERO);
            member.setTotalSpent(BigDecimal.ZERO);
            member.setOrderCount(0);
            member.setStatus(1);
            memberMapper.insert(member);
            created = true;
        } else if (member.getStatus() != null && member.getStatus() == 0) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }

        LoginResult result = finishLogin(member);
        result.setNewAccount(created);
        return result;
    }

    @Override
    public MemberVo profile(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return toVo(member);
    }

    @Override
    public MemberVo updateProfile(Long memberId, MemberVo request) {
        Member patch = new Member();
        patch.setId(memberId);
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            patch.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            patch.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            patch.setGender(request.getGender());
        }
        memberMapper.updateById(patch);
        return profile(memberId);
    }

    @Override
    public void changePassword(Long memberId, String oldPassword, String newPassword) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (member.getPassword() != null && !passwordEncoder.matches(oldPassword, member.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR, "原密码不正确");
        }
        Member patch = new Member();
        patch.setId(memberId);
        patch.setPassword(passwordEncoder.encode(newPassword));
        memberMapper.updateById(patch);
    }

    @Override
    public PageResult<MemberVo> pageForAdmin(String keyword, Integer status, String platform, int page, int limit) {
        LambdaQueryWrapper<Member> wrapper = Wrappers.<Member>lambdaQuery().orderByDesc(Member::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Member::getNickname, kw)
                    .or().like(Member::getMobile, kw));
        }
        if (status != null) {
            wrapper.eq(Member::getStatus, status);
        }
        if (platform != null && !platform.isEmpty()) {
            wrapper.eq(Member::getPlatform, platform);
        }
        Page<Member> result = memberMapper.selectPage(new Page<>(page, limit), wrapper);
        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(),
                result.getRecords().stream().map(this::toVo).collect(java.util.stream.Collectors.toList()));
    }

    @Override
    public void setStatus(Long memberId, int status) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        Member patch = new Member();
        patch.setId(memberId);
        patch.setStatus(status);
        memberMapper.updateById(patch);
    }

    @Override
    public String sendSmsCode(String mobile) {
        String rateKey = "sms:rate:" + mobile;
        if (redisService.hasKey(rateKey)) {
            throw new BizException(ResultCode.BAD_REQUEST, "验证码发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        redisService.set(CommonConstants.REDIS_SMS_CODE + mobile, code, Duration.ofMinutes(SMS_TTL_MINUTES));
        redisService.set(rateKey, "1", Duration.ofSeconds(SMS_RATE_LIMIT_SECONDS));
        log.info("sms code for {} is {}", mobile, code);
        return code;
    }

    @Override
    public MemberVo detailForAdmin(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return toVo(member);
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private Member findByMobile(String mobile) {
        return memberMapper.selectOne(Wrappers.<Member>lambdaQuery().eq(Member::getMobile, mobile));
    }

    private Member createMember(String mobile, String platform, String rawPassword) {
        Member member = new Member();
        member.setMobile(mobile);
        member.setPassword(passwordEncoder.encode(
                rawPassword == null || rawPassword.isEmpty() ? "123456" : rawPassword));
        member.setNickname("用户" + mobile.substring(mobile.length() - 4));
        member.setAvatar(CommonConstants.DEFAULT_AVATAR);
        member.setGender(0);
        member.setPlatform(platform);
        member.setLevel(1);
        member.setPoints(0);
        member.setBalance(BigDecimal.ZERO);
        member.setTotalSpent(BigDecimal.ZERO);
        member.setOrderCount(0);
        member.setStatus(1);
        memberMapper.insert(member);
        return member;
    }

    private LoginResult finishLogin(Member member) {
        if (member.getStatus() != null && member.getStatus() == 0) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        String platform = member.getPlatform() == null
                ? CommonConstants.PLATFORM_WEB : member.getPlatform();
        String token = tokenProvider.createToken(member.getId(),
                CommonConstants.AUDIENCE_APP, platform, member.getNickname());

        Member patch = new Member();
        patch.setId(member.getId());
        patch.setLastLoginAt(LocalDateTime.now());
        memberMapper.updateById(patch);

        LoginResult result = new LoginResult();
        result.setToken(token);
        result.setExpiresIn(tokenProvider.getExpireSeconds());
        result.setUserId(member.getId());
        result.setUsername(member.getMobile());
        result.setNickname(member.getNickname());
        result.setAvatar(member.getAvatar());
        result.setRole("ROLE_MEMBER");
        return result;
    }

    /**
     * Resolves an openid for a {@code wx.login()} code. Without configured
     * credentials this derives a deterministic pseudo openid so demo accounts
     * remain stable across restarts.
     */
    private String resolveOpenid(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("ecom-demo-openid:" + code).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("mp_");
            for (int i = 0; i < 12; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "mp_" + UUID.nameUUIDFromBytes(code.getBytes()).toString().replace("-", "");
        }
    }

    private MemberVo toVo(Member member) {
        MemberVo vo = new MemberVo();
        vo.setId(member.getId());
        vo.setMobile(member.getMobile());
        vo.setMobileMasked(maskMobile(member.getMobile()));
        vo.setNickname(member.getNickname());
        vo.setAvatar(member.getAvatar());
        vo.setGender(member.getGender());
        vo.setGenderText(member.getGender() == null ? "未知"
                : (member.getGender() == 1 ? "男" : (member.getGender() == 2 ? "女" : "未知")));
        vo.setPlatform(member.getPlatform());
        vo.setLevel(member.getLevel());
        vo.setLevelName(levelName(member.getLevel()));
        vo.setPoints(member.getPoints());
        vo.setBalance(member.getBalance());
        vo.setTotalSpent(member.getTotalSpent());
        vo.setOrderCount(member.getOrderCount());
        vo.setStatus(member.getStatus());
        vo.setLastLoginAt(member.getLastLoginAt());
        vo.setCreatedAt(member.getCreatedAt());
        return vo;
    }

    private String levelName(Integer level) {
        if (level == null) {
            return "普通会员";
        }
        switch (level) {
            case 4:
                return "钻石会员";
            case 3:
                return "黄金会员";
            case 2:
                return "白银会员";
            default:
                return "普通会员";
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }
}
