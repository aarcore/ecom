package com.ait.common.config;

import com.ait.common.api.R;
import com.ait.common.api.ResultCode;
import com.ait.common.utils.JsonUtils;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

/**
 * Cross-cutting HTTP plumbing:
 * <ul>
 *   <li>permissive CORS for the H5 storefront and the admin console</li>
 *   <li>a JSON 401 for requests rejected by Spring Security, keeping the
 *       response envelope identical to business errors</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class WebCommonConfiguration {

    @Bean
    public FilterRegistrationBean<CorsFilter> ecomCorsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        // credential-less CORS with an origin pattern keeps localhost, LAN and
        // any proxied deployment working without a config change
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    /**
     * Ensures auth failures raised before the MVC layer (Spring Security's
     * entry point) are rendered as {@code R} JSON instead of an HTML error page.
     */
    @Bean
    public FilterRegistrationBean<Filter> authFailureJsonFilter() {
        Filter filter = new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, javax.servlet.ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                if (request instanceof HttpServletRequest
                        && "OPTIONS".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
                    chain.doFilter(request, response);
                    return;
                }
                chain.doFilter(request, response);
                int status = httpResponse.getStatus();
                if ((status == HttpServletResponse.SC_UNAUTHORIZED
                        || status == HttpServletResponse.SC_FORBIDDEN)
                        && !httpResponse.isCommitted()) {
                    httpResponse.setStatus(HttpServletResponse.SC_OK);
                    httpResponse.setContentType("application/json;charset=UTF-8");
                    R<Void> body = status == HttpServletResponse.SC_UNAUTHORIZED
                            ? R.fail(ResultCode.UNAUTHORIZED)
                            : R.fail(ResultCode.FORBIDDEN);
                    response.getOutputStream().write(
                            JsonUtils.toJson(body).getBytes(StandardCharsets.UTF_8));
                }
            }
        };
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(filter);
        bean.addUrlPatterns("/api/*");
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return bean;
    }
}
