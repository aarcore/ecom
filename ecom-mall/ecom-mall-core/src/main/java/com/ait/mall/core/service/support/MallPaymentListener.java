package com.ait.mall.core.service.support;

import com.ait.pay.core.PayNotification;
import com.ait.pay.core.PayRequest;
import com.ait.pay.service.PaymentListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The mall's side of the gateway contract.
 *
 * <p>Its only real job is producing URLs: the gateway must not know how this
 * deployment is hosted, so the paths live here and the host comes from
 * configuration. Everything else is delegated to the order service so the
 * gateway keeps a one-way dependency on the commerce module.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MallPaymentListener implements PaymentListener {

    /**
     * Resolved lazily.
     *
     * <p>{@code OrderServiceImpl} depends on this listener (it needs callback
     * URLs when opening a payment), and this listener needs the order service to
     * settle it — a genuine cycle. Injecting the provider breaks it without
     * turning on {@code allow-circular-references}, which would hide real cycles
     * elsewhere as well.
     */
    private final ObjectProvider<com.ait.mall.core.service.OrderService> orderService;

    /**
     * Public base URL of this deployment, e.g. {@code https://shop.example.com}.
     *
     * <p>Channels require absolute callback URLs and cannot be given a relative
     * path, so this must be set from the environment in anything but local
     * development.
     */
    @Value("${ecom.pay.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    /** Where the browser lands after a redirect flow. */
    @Value("${ecom.pay.return-path:/orders/complete}")
    private String returnPath;

    @Override
    public String onPaid(PayNotification notification) {
        var service = orderService.getIfAvailable();
        if (service == null) {
            // can only happen during shutdown; the channel's retry will succeed
            log.warn("payment callback for {} arrived before the order service was ready",
                    notification.orderNo());
            return null;
        }
        return service.settleFromChannel(notification);
    }

    @Override
    public void onUnpaid(PayNotification notification) {
        // A closed or failed payment is not an error: the customer can simply
        // try again, and the order scheduler will close the order on schedule.
        log.debug("unpaid notification channel={} order={} status={}",
                notification.channelCode(), notification.orderNo(), notification.rawStatus());
    }

    @Override
    public String notifyUrlFor(String channelCode) {
        // one endpoint per channel so each can have its own signing secret and
        // so a misconfigured channel cannot receive another's payloads
        return trimBase() + "/api/app/pay/notify/" + channelCode;
    }

    @Override
    public String returnUrlFor(String channelCode, PayRequest request) {
        String url = trimBase() + returnPath;
        if (request != null && request.orderNo() != null) {
            url = url + "?orderNo=" + request.orderNo();
        }
        return url;
    }

    private String trimBase() {
        String base = publicBaseUrl == null ? "http://localhost:8080" : publicBaseUrl.trim();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
