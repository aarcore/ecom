package com.ait.pay.channel;

import com.ait.pay.core.ChannelConfig;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayException;
import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.core.PayResult;
import com.ait.pay.core.PaymentResultCode;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WeChat Pay (API v3) via Native pay.
 *
 * <p>Native rather than JSAPI because the storefront runs in a desktop browser:
 * a scanned QR works everywhere, including phones pointed at their own screen
 * partner. A mini program would use the JSAPI variant, which needs an extra
 * {@code openid} handshake and is a separate channel code.
 *
 * <p>Operational notes worth knowing when wiring this up:
 * <ul>
 *   <li>WeChat amounts are always <b>fen</b>, integer, so ¥32.99 is 3299.</li>
 *   <li>The API v3 certificate is downloaded automatically and refreshed in the
 *       background by {@code RSAAutoCertificateConfig}; no manual cron.</li>
 *   <li>Webhook payloads are AES-GCM encrypted; {@code NotificationParser}
 *       decrypts and verifies in one call, so we never parse the outer JSON by
 *       hand and never handle the plaintext resource ourselves.</li>
 *   <li>Success is {@code trade_state = SUCCESS}, not merely "notification
 *       received" — WeChat also notifies on failure states.</li>
 * </ul>
 *
 * <p>{@code RSAAutoCertificateConfig} performs an HTTP call on construction to
 * fetch certificates, so it is cached per credential fingerprint rather than
 * rebuilt per request: rebuilding it on every checkout would add a network
 * round trip to the payment path.
 */
@Slf4j
@Component
public class WechatPayChannel implements PayChannel {

    public static final String CODE = "wechat";

    private static final DateTimeFormatter EXPIRE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** Config cache keyed by credential fingerprint. */
    private final Map<String, RSAAutoCertificateConfig> configCache = new ConcurrentHashMap<>();

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "微信支付";
    }

    @Override
    public PayResult createPayment(PayRequest request, ChannelConfig config) {
        RSAAutoCertificateConfig sdkConfig = sdkConfig(config);

        PrepayRequest prepay = new PrepayRequest();
        prepay.setAppid(requireAppId(config));
        prepay.setMchid(requireMerchantId(config));
        prepay.setDescription(truncate(request.subject(), 127));
        prepay.setOutTradeNo(request.orderNo());
        prepay.setNotifyUrl(requireNotifyUrl(config));

        Amount amount = new Amount();
        amount.setTotal(toFen(request.amount()));
        amount.setCurrency(request.currency() == null ? "CNY" : request.currency());
        prepay.setAmount(amount);

        // WeChat rejects a time_expire in the past and caps it at 1 year. Align
        // with our 45-minute order timeout so the QR stops working when the
        // order dies, rather than leaving a payable code for a dead order.
        prepay.setTimeExpire(LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .plusMinutes(45)
                .format(EXPIRE_FORMAT));

        try {
            NativePayService service = new NativePayService.Builder()
                    .config(sdkConfig)
                    .build();
            String codeUrl = service.prepay(prepay).getCodeUrl();
            log.info("wechat native prepay created order={} fen={}", request.orderNo(), amount.getTotal());
            return PayResult.qr(CODE, request.orderNo(), codeUrl);
        } catch (Exception ex) {
            log.warn("wechat prepay failed order={}: {}", request.orderNo(), ex.getMessage());
            throw new PayException(PaymentResultCode.PAYMENT_FAILED,
                    "微信下单失败：" + safeMessage(ex));
        }
    }

    @Override
    public PayNotification parseNotification(Map<String, String> headers, String body,
                                             ChannelConfig config) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(header(headers, "Wechatpay-Serial"))
                .nonce(header(headers, "Wechatpay-Nonce"))
                .signature(header(headers, "Wechatpay-Signature"))
                .timestamp(header(headers, "Wechatpay-Timestamp"))
                .signType(header(headers, "Wechatpay-Signature-Type", "WECHATPAY2-SHA256-RSA2048"))
                .body(body)
                .build();

        Transaction transaction;
        try {
            NotificationParser parser = new NotificationParser(sdkConfig(config));
            // verifies the signature and decrypts the resource in one step
            transaction = parser.parse(requestParam, Transaction.class);
        } catch (Exception ex) {
            // a bad signature must never be treated as a payment result
            log.warn("wechat notification rejected: {}", ex.getMessage());
            throw new PayException(PaymentResultCode.SIGNATURE_INVALID);
        }

        boolean paid = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("mchid", transaction.getMchid());
        extra.put("appid", transaction.getAppid());
        if (transaction.getPayer() != null) {
            extra.put("openid", transaction.getPayer().getOpenid());
        }

        BigDecimal amount = transaction.getAmount() == null
                || transaction.getAmount().getTotal() == null
                ? null
                : BigDecimal.valueOf(transaction.getAmount().getTotal()).movePointLeft(2);

        return new PayNotification(
                CODE,
                transaction.getOutTradeNo(),
                transaction.getTransactionId(),
                amount,
                transaction.getAmount() == null ? "CNY" : transaction.getAmount().getCurrency(),
                paid,
                transaction.getTradeState() == null ? null : transaction.getTradeState().name(),
                extra);
    }

    @Override
    public String refund(String tradeNo, BigDecimal amount, String reason, ChannelConfig config) {
        // Refunds need the refunds API plus the original out_trade_no; the
        // channel only hands us the transaction id here, so a full
        // implementation belongs with the refund flow rather than the gateway.
        throw PayException.unsupported("微信退款需要在退款流程中按原商户单号发起");
    }

    @Override
    public String checkConfig(ChannelConfig config) {
        if (isBlank(config.appId())) {
            return "缺少 AppID";
        }
        if (isBlank(config.merchantId())) {
            return "缺少商户号 mchid";
        }
        if (isBlank(config.privateKey())) {
            return "缺少商户 API 私钥";
        }
        if (isBlank(config.apiKey())) {
            // stored in apiKey: the 32-char APIv3 key, not an access token
            return "缺少 APIv3 密钥";
        }
        if (isBlank(config.setting("merchantSerialNumber"))) {
            return "缺少商户证书序列号";
        }
        if (isBlank(config.notifyUrl())) {
            return "缺少回调地址 notify_url";
        }
        return null;
    }

    /** WeChat expects a JSON body; a non-2xx or a bare 4xx triggers a retry. */
    @Override
    public String notificationAck() {
        return "{\"code\":\"SUCCESS\",\"message\":\"成功\"}";
    }

    @Override
    public String notificationAckContentType() {
        return "application/json;charset=UTF-8";
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private RSAAutoCertificateConfig sdkConfig(ChannelConfig config) {
        // The certificate provider fetches over HTTP on construction, so reuse
        // one instance per credential set instead of paying that per checkout.
        String fingerprint = fingerprint(config);
        return configCache.computeIfAbsent(fingerprint, key -> {
            try {
                return new RSAAutoCertificateConfig.Builder()
                        .merchantId(requireMerchantId(config))
                        .privateKey(normalisePrivateKey(config.privateKey()))
                        .merchantSerialNumber(requireSetting(config, "merchantSerialNumber"))
                        .apiV3Key(requireApiKey(config))
                        .build();
            } catch (PayException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("wechat config init failed: {}", ex.getMessage());
                throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                        "微信支付配置无效：" + safeMessage(ex));
            }
        });
    }

    private static String fingerprint(ChannelConfig config) {
        // never log this; it only needs to change when credentials change
        return Integer.toHexString(java.util.Objects.hash(
                config.merchantId(), config.privateKey(), config.apiKey(),
                config.setting("merchantSerialNumber")));
    }

    /**
     * WeChat expects PEM, but operators routinely paste the base64 body from the
     * merchant portal without the header/footer. Normalise rather than fail.
     */
    private static String normalisePrivateKey(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "商户私钥未配置");
        }
        String key = raw.trim().replace("\\n", "\n");
        if (key.contains("BEGIN")) {
            return key;
        }
        String body = key.replaceAll("\\s+", "");
        StringBuilder pem = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < body.length(); i += 64) {
            pem.append(body, i, Math.min(i + 64, body.length())).append('\n');
        }
        return pem.append("-----END PRIVATE KEY-----").toString();
    }

    static int toFen(BigDecimal amount) {
        if (amount == null) {
            return 0;
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    private static String requireAppId(ChannelConfig config) {
        if (isBlank(config.appId())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "微信 AppID 未配置");
        }
        return config.appId();
    }

    private static String requireMerchantId(ChannelConfig config) {
        if (isBlank(config.merchantId())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "微信商户号未配置");
        }
        return config.merchantId();
    }

    private static String requireApiKey(ChannelConfig config) {
        if (isBlank(config.apiKey())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "微信 APIv3 密钥未配置");
        }
        return config.apiKey();
    }

    private static String requireSetting(ChannelConfig config, String key) {
        String value = config.setting(key);
        if (isBlank(value)) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "微信缺少配置项：" + key);
        }
        return value;
    }

    private static String requireNotifyUrl(ChannelConfig config) {
        if (isBlank(config.notifyUrl())) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED, "微信回调地址未配置");
        }
        return config.notifyUrl();
    }

    private static String header(Map<String, String> headers, String name) {
        return header(headers, name, null);
    }

    private static String header(Map<String, String> headers, String name, String fallback) {
        if (headers == null) {
            return fallback;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.length() > 160 ? "渠道返回异常" : message;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
