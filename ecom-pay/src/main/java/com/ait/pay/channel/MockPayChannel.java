package com.ait.pay.channel;

import com.ait.pay.core.ChannelConfig;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.core.PayResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * The demo channel.
 *
 * <p>It settles inline and never leaves the machine, which is what makes the
 * whole purchase flow testable without merchant credentials. It is a real
 * {@link PayChannel} rather than a branch in the order service, so the demo path
 * exercises exactly the same code as Stripe does — including the notification
 * handler — instead of a special case that could rot.
 *
 * <p>Because it is enabled by default in the seed data, a fresh clone can still
 * buy a bike with no configuration at all.
 */
@Slf4j
@Component
public class MockPayChannel implements PayChannel {

    public static final String CODE = "mock";

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String label() {
        return "模拟支付";
    }

    @Override
    public PayResult createPayment(PayRequest request, ChannelConfig config) {
        String tradeNo = "MOCK" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
        log.info("mock payment settled order={} amount={} tradeNo={}",
                request.orderNo(), request.amount(), tradeNo);
        return PayResult.sync(CODE, tradeNo, "模拟支付已完成");
    }

    /**
     * The mock channel has no webhook: it settles synchronously. Returning an
     * unpaid notification keeps the contract honest if somebody wires one up.
     */
    @Override
    public PayNotification parseNotification(Map<String, String> headers, String body,
                                             ChannelConfig config) {
        return new PayNotification(CODE, null, null, null, "CNY", false, "unsupported", Map.of());
    }

    @Override
    public String refund(String tradeNo, BigDecimal amount, String reason, ChannelConfig config) {
        return "MOCKREFUND" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @Override
    public String checkConfig(ChannelConfig config) {
        // nothing to configure: always healthy as long as it is enabled
        return null;
    }

    @Override
    public String notificationAck() {
        return "ok";
    }
}
