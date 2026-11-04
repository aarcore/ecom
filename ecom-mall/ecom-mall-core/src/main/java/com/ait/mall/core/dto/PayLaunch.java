package com.ait.mall.core.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * What the storefront gets when it asks to pay an order.
 *
 * <p>Mirrors {@code PayResult} but as a flat DTO: the client should not have to
 * know about the payment module's records, and this lets the mall add fields
 * (like the amount, which the checkout screen shows before redirecting) without
 * changing the gateway contract.
 */
@Data
public class PayLaunch {

    /** Our order reference, so the client can poll without threading it through. */
    private String orderNo;

    /** Channel the payment was opened on, e.g. {@code stripe}. */
    private String channelCode;

    /** Human readable channel name, already localised. */
    private String channelName;

    /** What the client should do next: REDIRECT / QR_CODE / SDK / SYNC. */
    private String mode;

    /** Redirect target, or an auto-submitting HTML form for Alipay. */
    private String url;

    /** Raw string to render as a QR, for QR_CODE mode. */
    private String qrContent;

    /** Extra fields for a platform SDK. */
    private Map<String, Object> sdkParams;

    /** The channel's transaction id, useful for support. */
    private String tradeNo;

    private BigDecimal amount;

    private String currency;

    /** True when the payment is already settled (mock channel). */
    private boolean paid;

    /** Short message, already localised. */
    private String message;

    /** Order status after the call, so the client can render straight away. */
    private Integer orderStatus;
}
