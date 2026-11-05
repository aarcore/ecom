package com.ait.pay.channel;

import com.ait.pay.core.ChannelConfig;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayException;
import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.core.PayResult;
import com.ait.pay.core.PaymentResultCode;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Alipay, desktop page pay (电脑网站支付).
 *
 * <p>Page pay returns a full HTML form that must be posted to Alipay, so the
 * result carries it as {@code url} — the storefront writes it into a hidden form
 * and submits it. That is Alipay's own documented flow; there is no redirect URL
 * to hand back for this product.
 *
 * <p>Notes that matter operationally:
 * <ul>
 *   <li>Signatures: the request is signed with our RSA2 private key, and every
 *       callback must be verified with <b>Alipay's</b> public key. Verifying
 *       means removing {@code sign} and {@code sign_type}, sorting the remaining
 *       parameters, and only then calling {@code rsaCheckV1}.</li>
 *   <li>The callback is form-encoded, not JSON, and amounts are in yuan as a
 *       string.</li>
 *   <li>Alipay expects the literal body {@code success} or it retries for a
 *       day.</li>
 *   <li>{@code trade_status = TRADE_SUCCESS} or {@code TRADE_FINISHED} both mean
 *       paid: the first is a normal payment, the second is a completed one that
 *       can no longer be refunded.</li>
 * </ul>
 */
@Slf4j
@Component
public class AlipayPayChannel implements PayChannel {

    public static final String CODE = "alipay";

    /** Alipay's sandbox gateway, used when the channel mode is not live. */
    private static final String SANDBOX_GATEWAY = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String LIVE_GATEWAY = "https://openapi.alipay.com/gateway.do";

    private static final DateTimeFormatter TIME_EXPIRE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "支付宝";
    }

    @Override
    public boolean requiresRedirect() {
        return true;
    }

    @Override
    public PayResult createPayment(PayRequest request, ChannelConfig config) {
        AlipayClient client = client(config);

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(request.orderNo());
        // Alipay wants yuan with at most two decimals as a string
        model.setTotalAmount(request.amount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        model.setSubject(truncate(request.subject(), 256));
        if (request.description() != null) {
            model.setBody(truncate(request.description(), 128));
        }
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        // match the 45 minute order timeout so a stale link cannot pay a dead order
        model.setTimeExpire(LocalDateTime.now().plusMinutes(45).format(TIME_EXPIRE));

        AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
        payRequest.setBizModel(model);
        payRequest.setNotifyUrl(requireNotifyUrl(config));
        if (config.returnUrl() != null && !config.returnUrl().isEmpty()) {
            payRequest.setReturnUrl(config.returnUrl());
        }

        try {
            AlipayTradePagePayResponse response = client.pageExecute(payRequest);
            if (!response.isSuccess()) {
                log.warn("alipay page pay refused order={} code={} sub={}",
                        request.orderNo(), response.getCode(), response.getSubMsg());
                throw new PayException(PaymentResultCode.PAYMENT_FAILED,
                        "支付宝下单失败：" + safe(response.getSubMsg(), response.getMsg()));
            }
            // pageExecute returns the auto-submitting form in `body`
            return PayResult.redirect(CODE, request.orderNo(), response.getBody());
        } catch (PayException ex) {
            throw ex;
        } catch (AlipayApiException ex) {
            log.warn("alipay createPayment failed order={}: {}", request.orderNo(), ex.getErrMsg());
            throw new PayException(PaymentResultCode.PAYMENT_FAILED,
                    "支付宝下单失败：" + safe(ex.getErrMsg(), "渠道返回异常"));
        }
    }

    @Override
    public PayNotification parseNotification(Map<String, String> headers, String body,
                                             ChannelConfig config) {
        // The body is form-encoded; the caller has already parsed it into the
        // header map's companion, so re-parse here to keep the interface small.
        Map<String, String> params = parseForm(body);
        if (params.isEmpty()) {
            throw new PayException(PaymentResultCode.NOTIFICATION_INVALID);
        }

        String signType = params.getOrDefault("sign_type", "RSA2");
        try {
            // Alipay's own public key verifies what Alipay signed.
            boolean valid = AlipaySignature.rsaCheckV1(
                    params,
                    config.publicKey(),
                    params.getOrDefault("charset", StandardCharsets.UTF_8.name()),
                    signType);
            if (!valid) {
                throw new PayException(PaymentResultCode.SIGNATURE_INVALID);
            }
        } catch (PayException ex) {
            throw ex;
        } catch (AlipayApiException ex) {
            log.warn("alipay notification rejected: {}", ex.getErrMsg());
            throw new PayException(PaymentResultCode.SIGNATURE_INVALID);
        }

        String tradeStatus = params.get("trade_status");
        boolean paid = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("buyerId", params.get("buyer_id"));
        extra.put("sellerId", params.get("seller_id"));
        extra.put("gmtPayment", params.get("gmt_payment"));
        extra.put("appId", params.get("app_id"));

        return new PayNotification(
                CODE,
                params.get("out_trade_no"),
                params.get("trade_no"),
                parseAmount(params.get("total_amount")),
                "CNY",
                paid,
                tradeStatus,
                extra);
    }

    @Override
    public String refund(String tradeNo, BigDecimal amount, String reason, ChannelConfig config) {
        AlipayClient client = client(config);
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        // tradeNo here is our out_trade_no, which is what this adapter stores
        model.setOutTradeNo(tradeNo);
        if (amount != null) {
            model.setRefundAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        }
        if (reason != null && !reason.isEmpty()) {
            model.setRefundReason(truncate(reason, 256));
        }

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);
        try {
            var response = client.execute(request);
            if (!response.isSuccess()) {
                throw new PayException(PaymentResultCode.REFUND_FAILED,
                        "支付宝退款失败：" + safe(response.getSubMsg(), response.getMsg()));
            }
            return response.getTradeNo();
        } catch (PayException ex) {
            throw ex;
        } catch (AlipayApiException ex) {
            throw new PayException(PaymentResultCode.REFUND_FAILED,
                    "支付宝退款失败：" + safe(ex.getErrMsg(), "渠道返回异常"));
        }
    }

    @Override
    public String checkConfig(ChannelConfig config) {
        if (isBlank(config.appId())) {
            return "缺少应用 AppID";
        }
        if (isBlank(config.privateKey())) {
            return "缺少应用私钥";
        }
        if (isBlank(config.publicKey())) {
            return "缺少支付宝公钥（用于校验回调）";
        }
        if (isBlank(config.notifyUrl())) {
            return "缺少异步通知地址 notify_url";
        }
        return null;
    }

    /** Alipay only treats the literal {@code success} as an acknowledgement. */
    @Override
    public String notificationAck() {
        return "success";
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    /**
     * Builds a client per call. {@code DefaultAlipayClient} is cheap and keeps
     * an internal connection pool, but caching per credential set risks holding
     * a stale key after an operator rotates it in the console.
     */
    private AlipayClient client(ChannelConfig config) {
        try {
            AlipayConfig alipayConfig = new AlipayConfig();
            alipayConfig.setServerUrl(gateway(config));
            alipayConfig.setAppId(requireAppId(config));
            alipayConfig.setPrivateKey(normaliseKey(config.privateKey(), "PRIVATE KEY"));
            alipayConfig.setAlipayPublicKey(normaliseKey(config.publicKey(), "PUBLIC KEY"));
            alipayConfig.setFormat("json");
            alipayConfig.setCharset(StandardCharsets.UTF_8.name());
            alipayConfig.setSignType("RSA2");
            return new DefaultAlipayClient(alipayConfig);
        } catch (PayException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("alipay client init failed: {}", ex.getMessage());
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "支付宝配置无效：" + safe(ex.getMessage(), "初始化失败"));
        }
    }

    private String gateway(ChannelConfig config) {
        // explicit override wins, then sandbox for non-live modes
        String override = config.setting("gateway");
        if (!isBlank(override)) {
            return override;
        }
        return "live".equalsIgnoreCase(config.mode()) ? LIVE_GATEWAY : SANDBOX_GATEWAY;
    }

    /**
     * Operators paste keys in every shape imaginable. The SDK accepts PEM, so
     * wrap a bare base64 body rather than rejecting it.
     */
    private static String normaliseKey(String raw, String keyType) {
        if (isBlank(raw)) {
            return raw;
        }
        String key = raw.trim().replace("\\n", "\n");
        if (key.contains("BEGIN")) {
            return key;
        }
        String body = key.replaceAll("\\s+", "");
        StringBuilder pem = new StringBuilder("-----BEGIN " + keyType + "-----\n");
        for (int i = 0; i < body.length(); i += 64) {
            pem.append(body, i, Math.min(i + 64, body.length())).append('\n');
        }
        return pem.append("-----END ").append(keyType).append("-----").toString();
    }

    /** Parses an {@code a=1&b=2} body, URL-decoding values. */
    private static Map<String, String> parseForm(String body) {
        Map<String, String> result = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) {
            return result;
        }
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = decode(pair.substring(0, idx));
            String value = decode(pair.substring(idx + 1));
            result.put(key, value);
        }
        return result;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }

    private static BigDecimal parseAmount(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String requireAppId(ChannelConfig config) {
        if (isBlank(config.appId())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "支付宝 AppID 未配置");
        }
        return config.appId();
    }

    private static String requireNotifyUrl(ChannelConfig config) {
        if (isBlank(config.notifyUrl())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "支付宝回调地址未配置");
        }
        return config.notifyUrl();
    }

    private static String safe(String primary, String fallback) {
        String value = primary != null && !primary.isEmpty() ? primary : fallback;
        return value == null || value.length() > 160 ? "渠道返回异常" : value;
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
