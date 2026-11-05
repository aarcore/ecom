package com.ait.app.config;

import com.ait.common.security.JwtAuthInterceptor;
import com.ait.common.security.JwtProperties;
import com.ait.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards the C-end API. Catalogue browsing plus the login endpoints stay open;
 * everything else requires an {@code app} audience token.
 */
@Configuration
@RequiredArgsConstructor
public class AppWebConfig implements WebMvcConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> whitelist = new ArrayList<>(jwtProperties.getAppWhitelist());
        whitelist.add("/api/app/auth/login");
        whitelist.add("/api/app/auth/login/mp");
        whitelist.add("/api/app/auth/sms");
        whitelist.add("/api/app/home");
        whitelist.add("/api/app/products/**");
        whitelist.add("/api/app/categories");
        whitelist.add("/api/app/locales");
        // payment callbacks authenticate with their own signature, not a token
        whitelist.add("/api/app/pay/notify/**");
        whitelist.add("/api/app/pay/return");
        whitelist.add("/api/app/brands");
        whitelist.add("/api/app/banners");
        whitelist.add("/api/app/notices");
        whitelist.add("/api/app/coupons/available");

        registry.addInterceptor(JwtAuthInterceptor.app(tokenProvider, whitelist))
                .addPathPatterns("/api/app/**");
    }
}
