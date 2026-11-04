package com.ait.pay.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime configuration for one channel, as stored in {@code shop_pay_channel}.
 *
 * <p>Credentials live in the database rather than {@code application.yml}
 * because the admin console has to let an operator paste a new secret key
 * without a redeploy — that is the whole point of the payment module in the
 * console. They are never echoed back: {@code ConfigService} masks them on read
 * and the API returns only a "configured / not configured" flag.
 *
 * <p>{@code settings} holds the non-secret knobs that differ per provider
 * (Stripe publishable key, Alipay gateway URL, WeChat appid, ...). Using a map
 * keeps the table stable when a provider needs a new field: the adapter reads
 * the keys it knows and ignores the rest.
 */
public record ChannelConfig(
        String code,
        boolean enabled,
        String mode,
        String appId,
        String merchantId,
        String privateKey,
        String publicKey,
        String apiKey,
        String apiSecret,
        String notifyUrl,
        String returnUrl,
        Map<String, String> settings) {

    public ChannelConfig {
        settings = settings == null ? Collections.emptyMap() : Map.copyOf(settings);
    }

    public String setting(String key) {
        return settings.get(key);
    }

    public String setting(String key, String fallback) {
        String value = settings.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    public boolean enabled() {
        return enabled;
    }

    /** Builds a config for tests and for the mock channel. */
    public static ChannelConfig of(String code, boolean enabled) {
        return new ChannelConfig(code, enabled, "test", null, null, null, null,
                null, null, null, null, new HashMap<>());
    }
}
