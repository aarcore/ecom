package com.ait.common.constant;

/**
 * Shared constants for the whole platform.
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** Audience claim value for the back-office console. */
    public static final String AUDIENCE_ADMIN = "admin";

    /** Audience claim value for C-end clients (web app + mini program). */
    public static final String AUDIENCE_APP = "app";

    /** Platform identifiers carried in the header {@code X-Client-Platform}. */
    public static final String PLATFORM_WEB = "web";
    public static final String PLATFORM_MP = "mp";

    public static final String HEADER_TOKEN = "Authorization";
    public static final String HEADER_PLATFORM = "X-Client-Platform";
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Redis keys. */
    public static final String REDIS_SMS_CODE = "sms:code:";
    public static final String REDIS_PAY_NOTIFY = "pay:notify:";
    public static final String REDIS_ADMIN_TOKEN_BLACKLIST = "token:blacklist:admin:";
    public static final String REDIS_APP_TOKEN_BLACKLIST = "token:blacklist:app:";

    /** Order life-cycle keys used by delayed cancellation jobs. */
    public static final String REDIS_ORDER_UNPAID = "order:unpaid:";

    public static final String DEFAULT_AVATAR =
            "https://api.dicebear.com/7.x/notionists/svg?seed=ecom-user";

    /** Soft delete flag values. */
    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;
}
