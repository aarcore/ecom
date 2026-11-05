package com.ait.pay.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The set of channels this build can speak.
 *
 * <p>Discovered from Spring, so a provider that lives in a separate jar and is
 * on the classpath appears here with no registration step. Ordering follows
 * {@link PayChannel#code()} so the storefront shows a stable list. It only
 * knows classes, not configuration — whether a channel is actually usable is a
 * database fact, resolved by {@code PayChannelService}.
 */
@Slf4j
@Component
public class PayChannelRegistry {

    private final Map<String, PayChannel> channels = new LinkedHashMap<>();

    public PayChannelRegistry(List<PayChannel> discovered) {
        discovered.stream()
                .sorted(java.util.Comparator.comparing(PayChannel::code))
                .forEach(channel -> {
                    PayChannel previous = channels.put(channel.code(), channel);
                    if (previous != null) {
                        // a duplicate code would silently shadow one provider;
                        // fail at startup instead of at 3am
                        throw new IllegalStateException("duplicate payment channel code: "
                                + channel.code() + " (" + previous.getClass().getName()
                                + " vs " + channel.getClass().getName() + ")");
                    }
                    log.info("payment channel registered: {} ({})", channel.code(), channel.label());
                });
    }

    public Optional<PayChannel> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(channels.get(code.toLowerCase()));
    }

    /** Like {@link #find} but throws a customer-safe error. */
    public PayChannel require(String code) {
        return find(code).orElseThrow(() -> PayException.unsupported("不支持的支付渠道：" + code));
    }

    public Collection<PayChannel> all() {
        return channels.values();
    }

    public boolean exists(String code) {
        return find(code).isPresent();
    }
}
