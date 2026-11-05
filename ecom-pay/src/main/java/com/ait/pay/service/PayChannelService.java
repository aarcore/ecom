package com.ait.pay.service;

import com.ait.pay.core.ChannelConfig;

import java.util.List;
import java.util.Optional;

/**
 * Reads and writes {@code shop_pay_channel}.
 *
 * <p>Two audiences with different needs:
 * <ul>
 *   <li>the order flow wants {@link #resolve(String)} — a decoded config for a
 *       channel that is switched on, or a clear error</li>
 *   <li>the admin console wants the full list including disabled channels and
 *       without any secret in the payload</li>
 * </ul>
 */
public interface PayChannelService {

    /** Every configured channel, sorted for display. Secrets are stripped. */
    List<PayChannelView> listAll();

    /**
     * Channels a customer may pay with right now: switched on, credentials
     * present, and backed by an adapter in this build.
     */
    List<PayChannelView> listAvailable();

    /**
     * Decoded configuration for a channel, or empty when the row is missing or
     * switched off. Used by the order flow.
     */
    Optional<ChannelConfig> resolve(String code);

    /** Same as {@link #resolve} but throws a customer-safe error. */
    ChannelConfig requireEnabled(String code);

    /**
     * Decoded configuration regardless of the enabled flag.
     *
     * <p>Needed by the callback path: a customer who paid moments before an
     * operator switched a channel off still has a valid payment, and we must be
     * able to verify that notification and settle the order.
     */
    Optional<ChannelConfig> resolveIncludingDisabled(String code);

    /** Raw row, secrets still encrypted. For internal updates only. */
    Optional<com.ait.pay.entity.PayChannelEntity> findEntity(String code);

    /** Creates or updates a channel. Blank secrets keep their stored value. */
    PayChannelView save(PayChannelInput input);

    /** Flips the switch without touching any other field. */
    PayChannelView setEnabled(String code, boolean enabled);

    void delete(String code);

    /**
     * A view model: everything an operator needs to see, nothing they should
     * not. Secrets appear only as {@code hasPrivateKey} style booleans.
     */
    record PayChannelView(
            Long id,
            String code,
            String name,
            String provider,
            boolean enabled,
            String mode,
            String appId,
            String merchantId,
            boolean hasPrivateKey,
            boolean hasPublicKey,
            boolean hasApiKey,
            boolean hasApiSecret,
            String notifyUrl,
            String returnUrl,
            java.util.Map<String, String> settings,
            Integer sort,
            String remark,
            boolean supported,
            /** Set when the adapter reports a configuration problem. */
            String healthMessage) {
    }

    /** Write model for the console. Blank secret fields mean "leave unchanged". */
    record PayChannelInput(
            String code,
            String name,
            String provider,
            Boolean enabled,
            String mode,
            String appId,
            String merchantId,
            String privateKey,
            String publicKey,
            String apiKey,
            String apiSecret,
            String notifyUrl,
            String returnUrl,
            java.util.Map<String, String> settings,
            Integer sort,
            String remark) {
    }
}
