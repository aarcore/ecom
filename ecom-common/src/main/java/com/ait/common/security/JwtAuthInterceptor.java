package com.ait.common.security;

import com.ait.common.api.ResultCode;
import com.ait.common.constant.CommonConstants;
import com.ait.common.exception.BizException;
import com.ait.common.utils.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;

/**
 * Authenticates requests by audience. Two instances exist — one guarding
 * {@code /api/admin/**} and one guarding {@code /api/app/**} — so a customer
 * token can never reach an operator endpoint even if path prefixes were
 * rearranged later.
 *
 * <p>Whitelisting is handled here (rather than in a Spring Security filter
 * chain) because the mini program sometimes transports the token as a query
 * parameter, which is simplest to express at the MVC layer.
 */
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final JwtTokenProvider tokenProvider;
    private final String expectedAudience;
    private final List<String> whitelist;

    public JwtAuthInterceptor(JwtTokenProvider tokenProvider, String expectedAudience) {
        this(tokenProvider, expectedAudience, Collections.emptyList());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = RequestUtils.resolveToken(request);

        if (isWhitelisted(request.getRequestURI())) {
            // still populate the principal so "optional auth" endpoints can show
            // member specific decorations (favourite state, cart badge, ...)
            LoginUser optional = tokenProvider.resolve(token, expectedAudience);
            if (optional != null) {
                LoginUserContext.set(optional);
            }
            return true;
        }

        LoginUser user = tokenProvider.resolve(token, expectedAudience);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        LoginUserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    /**
     * Extracts the path relative to the servlet context so whitelist entries are
     * written without the context prefix.
     */
    private boolean isWhitelisted(String uri) {
        String path = uri;
        int idx = path.indexOf("/api/");
        if (idx > 0) {
            path = path.substring(idx);
        }
        for (String pattern : whitelist) {
            if (MATCHER.match(pattern, path) || path.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    public String getExpectedAudience() {
        return expectedAudience;
    }

    public static JwtAuthInterceptor admin(JwtTokenProvider provider, List<String> whitelist) {
        return new JwtAuthInterceptor(provider, CommonConstants.AUDIENCE_ADMIN, whitelist);
    }

    public static JwtAuthInterceptor app(JwtTokenProvider provider, List<String> whitelist) {
        return new JwtAuthInterceptor(provider, CommonConstants.AUDIENCE_APP, whitelist);
    }
}
