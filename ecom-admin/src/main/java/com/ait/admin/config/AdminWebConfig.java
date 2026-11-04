package com.ait.admin.config;

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
 * Guards every {@code /api/admin/**} endpoint with the {@code admin} audience.
 */
@Configuration
@RequiredArgsConstructor
public class AdminWebConfig implements WebMvcConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> whitelist = new ArrayList<>(jwtProperties.getAdminWhitelist());
        whitelist.add("/api/admin/auth/login");

        registry.addInterceptor(JwtAuthInterceptor.admin(tokenProvider, whitelist))
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
    }
}
