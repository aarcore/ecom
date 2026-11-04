package com.ait.common.utils;

import javax.servlet.http.HttpServletRequest;

/**
 * Small helpers for reading the transport level auth headers.
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(com.ait.common.constant.CommonConstants.HEADER_TOKEN);
        if (header == null || header.isEmpty()) {
            // mini programs often cannot set custom auth headers everywhere,
            // so also accept the token via query string on GET requests
            return request.getParameter("token");
        }
        if (header.startsWith(com.ait.common.constant.CommonConstants.TOKEN_PREFIX)) {
            return header.substring(com.ait.common.constant.CommonConstants.TOKEN_PREFIX.length()).trim();
        }
        return header.trim();
    }

    public static String resolvePlatform(HttpServletRequest request) {
        String platform = request.getHeader(com.ait.common.constant.CommonConstants.HEADER_PLATFORM);
        if (platform == null || platform.isEmpty()) {
            String ua = request.getHeader("User-Agent");
            if (ua != null && ua.toLowerCase().contains("miniprogram")) {
                return com.ait.common.constant.CommonConstants.PLATFORM_MP;
            }
            return com.ait.common.constant.CommonConstants.PLATFORM_WEB;
        }
        return platform;
    }

    public static String clientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                int comma = value.indexOf(',');
                return comma > 0 ? value.substring(0, comma).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
