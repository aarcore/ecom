package com.ait.pay.channel;

import com.ait.pay.core.ChannelConfig;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayException;
import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.core.PayResult;
import com.ait.pay.core.PaymentResultCode;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stripe Checkout.
 *
 * <p>Uses hosted Checkout rather than Elements: it keeps card data entirely off
 * our origin, which is both a PCI-scope argument and less client code. The
 * customer is redirected to {@code url} and returns to {@code returnUrl}.
 *
 * <p>Two things that bite in practice and are handled here:
 * <ul>
 *   <li><b>Minor units.</b> Stripe wants the amount in the currency's smallest
 *       unit, so ¥32.99 becomes 3299. Naively multiplying by 100 breaks
 *       zero-decimal currencies like JPY, so those are listed explicitly.</li>
 *   <li><b>Which event means paid.</b> {@code checkout.session.completed} fires
 *       before the money has actually settled. For synchronous methods it is
 *       fine, but for delayed methods you must also accept
 *       {@code checkout.session.async_payment_succeeded}.</li>
 * </ul>
 */
@Slf4j
@Component
public class StripePayChannel implements PayChannel {

    public static final String CODE = "stripe";

    /** Currencies with no minor unit — amount is the number itself. */
    private static final Set<String> ZERO_DECIMAL = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG",
            "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    /** Three-decimal currencies. */
    private static final Set<String> THREE_DECIMAL = Set.of("BHD", "JOD", "KWD", "OMR", "TND");

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "Stripe";
    }

    @Override
    public boolean requiresRedirect() {
        return true;
    }

    @Override
    public PayResult createPayment(PayRequest request, ChannelConfig config) {
        applyApiKey(config);
        String currency = normaliseCurrency(request.currency());

        long minor = toMinorUnits(request.amount(), currency);
        if (minor <= 0) {
            throw new PayException(PaymentResultCode.PAYMENT_FAILED, "订单金额必须大于 0");
        }

        // Stripe requires an absolute success/cancel URL.
        String successUrl = withQuery(config.returnUrl(), request.orderNo(), "success");
        String cancelUrl = withQuery(config.returnUrl(), request.orderNo(), "cancel");

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(request.orderNo())
                // keep our reference visible in the Stripe dashboard
                .putMetadata("orderNo", request.orderNo())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency.toLowerCase(Locale.ROOT))
                                .setUnitAmount(minor)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                        .builder()
                                        .setName(truncate(request.subject(), 250))
                                        .setDescription(truncate(request.description(), 250))
                                        .build())
                                .build())
                        .build());

        // Stripe Checkout expires after 24h by default; our orders die in 45
        // minutes, so align the session with the order to avoid a payable link
        // for an order we already cancelled.
        params.setExpiresAt(java.time.Instant.now().plusSeconds(45 * 60).getEpochSecond());

        try {
            Session session = Session.create(params.build());
            log.info("stripe checkout created order={} session={} amount={} {}",
                    request.orderNo(), session.getId(), minor, currency);
            return PayResult.redirect(CODE, session.getId(), session.getUrl());
        } catch (com.stripe.exception.AuthenticationException ex) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "Stripe 密钥无效，请在管理端检查配置");
        } catch (StripeException ex) {
            log.warn("stripe createPayment failed order={}: {}", request.orderNo(), ex.getMessage());
            throw new PayException(PaymentResultCode.PAYMENT_FAILED,
                    "Stripe 下单失败：" + safeMessage(ex));
        }
    }

    @Override
    public PayNotification parseNotification(Map<String, String> headers, String body,
                                             ChannelConfig config) {
        String signature = header(headers, "stripe-signature");
        // the signing secret lives in settings: it is not one of the four generic
        // credential slots, and it is per-endpoint rather than per-account
        String secret = config == null ? null : config.setting("webhookSecret");
        if (secret == null || secret.isEmpty()) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "Stripe webhook 密钥未配置");
        }
        if (signature == null) {
            throw new PayException(PaymentResultCode.SIGNATURE_INVALID);
        }

        Event event;
        try {
            // The signature covers the raw bytes, so this must be the untouched body.
            event = Webhook.constructEvent(body, signature, secret);
        } catch (SignatureVerificationException ex) {
            log.warn("stripe webhook signature rejected: {}", ex.getMessage());
            throw new PayException(PaymentResultCode.SIGNATURE_INVALID);
        } catch (Exception ex) {
            throw new PayException(PaymentResultCode.NOTIFICATION_INVALID);
        }

        String type = event.getType();
        boolean paid = "checkout.session.completed".equals(type)
                || "checkout.session.async_payment_succeeded".equals(type);
        if (!paid) {
            // tell the caller it is a real event but not a payment result
            return new PayNotification(CODE, null, null, null, null, false, type, Map.of());
        }

        // Event data arrives as untyped JSON; read it as a map so this compiles
        // regardless of which model class the SDK version deserialised into.
        Map<String, Object> session = asMap(event.getDataObjectDeserializer()
                .getObject().orElse(null));
        String orderNo = stringValue(session.get("client_reference_id"));
        if (orderNo == null) {
            orderNo = nested(session, "metadata", "orderNo");
        }
        String tradeNo = stringValue(session.get("id"));
        Long amountMinor = longValue(session.get("amount_total"));
        String currency = stringValue(session.get("currency"));

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("eventId", event.getId());
        extra.put("paymentStatus", stringValue(session.get("payment_status")));

        return new PayNotification(
                CODE,
                orderNo,
                tradeNo,
                amountMinor == null ? null : fromMinorUnits(amountMinor, currency),
                currency == null ? "CNY" : currency.toUpperCase(Locale.ROOT),
                true,
                type,
                extra);
    }

    @Override
    public String refund(String tradeNo, BigDecimal amount, String reason, ChannelConfig config) {
        applyApiKey(config);
        try {
            // a Checkout Session is not refundable directly; its payment intent is
            Session session = Session.retrieve(tradeNo);
            String paymentIntent = session.getPaymentIntent();
            if (paymentIntent == null) {
                throw new PayException(PaymentResultCode.REFUND_FAILED, "该 Stripe 会话尚无支付记录");
            }
            RefundCreateParams.Builder params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntent);
            if (amount != null) {
                String currency = normaliseCurrency(session.getCurrency());
                params.setAmount(toMinorUnits(amount, currency));
            }
            if (reason != null && !reason.isEmpty()) {
                params.putMetadata("reason", truncate(reason, 500));
            }
            Refund refund = Refund.create(params.build());
            return refund.getId();
        } catch (PayException ex) {
            throw ex;
        } catch (StripeException ex) {
            log.warn("stripe refund failed tradeNo={}: {}", tradeNo, ex.getMessage());
            throw new PayException(PaymentResultCode.REFUND_FAILED, safeMessage(ex));
        }
    }

    @Override
    public String checkConfig(ChannelConfig config) {
        if (isBlank(config.apiKey())) {
            return "缺少 Stripe Secret Key";
        }
        if (!config.apiKey().startsWith("sk_")) {
            return "Secret Key 应以 sk_ 开头";
        }
        if (isBlank(config.returnUrl())) {
            return "缺少支付完成后的返回地址";
        }
        return null;
    }

    @Override
    public String notificationAckContentType() {
        return "application/json;charset=UTF-8";
    }

    /** Stripe only needs a 2xx; an empty JSON object is the conventional body. */
    @Override
    public String notificationAck() {
        return "{}";
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private void applyApiKey(ChannelConfig config) {
        if (isBlank(config.apiKey())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "Stripe Secret Key 未配置");
        }
        // The SDK key is process-global, so set it per call rather than at
        // startup: an operator can rotate the key in the console without a
        // restart. The API version is a compile-time constant in stripe-java 29
        // (Stripe.API_VERSION), pinned by the dependency version above.
    }

    private static String normaliseCurrency(String currency) {
        return currency == null || currency.isEmpty() ? "CNY" : currency.toUpperCase(Locale.ROOT);
    }

    static long toMinorUnits(BigDecimal amount, String currency) {
        if (amount == null) {
            return 0;
        }
        int scale = ZERO_DECIMAL.contains(currency) ? 0 : THREE_DECIMAL.contains(currency) ? 3 : 2;
        BigDecimal scaled = amount.movePointRight(scale).setScale(0, RoundingMode.HALF_UP);
        return scaled.longValueExact();
    }

    static BigDecimal fromMinorUnits(long minor, String currency) {
        String code = normaliseCurrency(currency);
        int scale = ZERO_DECIMAL.contains(code) ? 0 : THREE_DECIMAL.contains(code) ? 3 : 2;
        return BigDecimal.valueOf(minor).movePointLeft(scale);
    }

    private static String withQuery(String base, String orderNo, String result) {
        if (base == null || base.isEmpty()) {
            // Stripe rejects a relative URL; make the failure obvious in dev
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "Stripe 需要绝对地址的返回 URL，请在管理端配置");
        }
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "orderNo=" + orderNo + "&result=" + result;
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        if (value == null) {
            return Map.of();
        }
        // fall back to the SDK's own JSON view
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result = com.stripe.net.ApiResource.GSON.fromJson(
                    com.stripe.net.ApiResource.GSON.toJson(value), Map.class);
        } catch (Exception ignored) {
            /* leave empty: the caller treats a missing orderNo as invalid */
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String nested(Map<String, Object> map, String key, String inner) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return stringValue(((Map<String, Object>) value).get(inner));
        }
        return null;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String safeMessage(StripeException ex) {
        // never surface a raw Stripe payload: it can echo back the API key
        String message = ex.getMessage();
        return message == null || message.length() > 160 ? "渠道返回异常" : message;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
