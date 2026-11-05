package com.ait.pay.service.impl;

import com.ait.pay.core.ChannelConfig;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayChannelRegistry;
import com.ait.pay.core.PayException;
import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.core.PayResult;
import com.ait.pay.core.PaymentResultCode;
import com.ait.pay.service.PayChannelService;
import com.ait.pay.service.PayGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Resolves the channel, then hands off.
 *
 * <p>Everything provider specific lives in a {@code PayChannel}; everything
 * common — is it enabled, is it configured, what error do we show, what does the
 * callback acknowledge — lives here. That split is what makes adding a provider
 * a one-class change.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayGatewayImpl implements PayGateway {

    private final PayChannelRegistry registry;
    private final PayChannelService channelService;

    @Override
    public PayResult create(String channelCode, PayRequest request) {
        PayChannel channel = registry.require(channelCode);
        ChannelConfig config = channelService.requireEnabled(channelCode);

        // fail before touching the network if the operator left a required field
        String problem = channel.checkConfig(config);
        if (problem != null) {
            log.warn("channel {} is enabled but not usable: {}", channelCode, problem);
            throw new PayException(PaymentResultCode.CHANNEL_NOT_CONFIGURED,
                    "支付渠道「" + channel.label() + "」尚未配置完成：" + problem);
        }

        log.info("opening payment channel={} order={} amount={} {}",
                channelCode, request.orderNo(), request.amount(), request.currency());
        return channel.createPayment(request, config);
    }

    @Override
    public PayNotification verify(String channelCode, Map<String, String> headers, String body) {
        PayChannel channel = registry.require(channelCode);
        // A callback for a disabled channel still has to be verified: the customer
        // may have paid just before the operator switched it off, and dropping
        // that payment would leave a paid order stuck in pending.
        ChannelConfig config = channelService.resolveIncludingDisabled(channelCode)
                .orElseThrow(() -> new PayException(PaymentResultCode.CHANNEL_NOT_FOUND));

        PayNotification notification = channel.parseNotification(headers, body, config);
        log.info("payment callback channel={} order={} trade={} paid={} status={}",
                channelCode, notification.orderNo(), notification.tradeNo(),
                notification.paid(), notification.rawStatus());
        return notification;
    }

    @Override
    public String refund(String channelCode, String tradeNo, java.math.BigDecimal amount,
                         String reason) {
        PayChannel channel = registry.require(channelCode);
        ChannelConfig config = channelService.resolveIncludingDisabled(channelCode)
                .orElseThrow(() -> new PayException(PaymentResultCode.CHANNEL_NOT_FOUND));
        log.info("refunding channel={} tradeNo={} amount={}", channelCode, tradeNo, amount);
        return channel.refund(tradeNo, amount, reason, config);
    }

    @Override
    public Ack ack(String channelCode) {
        PayChannel channel = registry.find(channelCode).orElse(null);
        if (channel == null) {
            return new Ack("success", "text/plain;charset=UTF-8");
        }
        return new Ack(channel.notificationAck(), channel.notificationAckContentType());
    }

    @Override
    public void assertAvailable(String channelCode) {
        channelService.requireEnabled(channelCode);
        registry.require(channelCode);
    }
}
