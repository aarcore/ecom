package com.ait.common.security;

import com.ait.common.api.ResultCode;
import com.ait.common.exception.BizException;

/**
 * Thread-bound holder for the current principal. Cleared by the interceptors in
 * an {@code afterCompletion} callback so pooled request threads never leak an
 * identity.
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * @throws BizException with 401 semantics when there is no principal.
     */
    public static LoginUser required() {
        LoginUser user = HOLDER.get();
        if (user == null || user.getUserId() == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long requiredUserId() {
        return required().getUserId();
    }

    public static Long userIdOrNull() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }
}
