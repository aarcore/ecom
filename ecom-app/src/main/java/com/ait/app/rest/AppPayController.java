package com.ait.app.rest;

import com.ait.common.api.R;
import com.ait.common.security.LoginUserContext;
import com.ait.common.utils.RequestUtils;
import com.ait.mall.core.dto.PayLaunch;
import com.ait.mall.core.service.OrderService;
import com.ait.pay.core.PayNotification;
import com.ait.pay.service.PayChannelService;
import com.ait.pay.service.PayGateway;
import com.ait.pay.service.PayRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Storefront payment endpoints.
 *
 * <p>Two halves that look alike but have completely different trust models:
 * <ul>
 *   <li>{@code /channels} and {@code /orders/{no}/pay} are called by the browser
 *       and require a customer token</li>
 *   <li>{@code /notify/{channel}} is called by the payment provider and must be
 *       reachable without a token — the signature in the payload is the
 *       authentication. It is therefore whitelisted in {@code AppWebConfig}.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/app/pay")
@RequiredArgsConstructor
@Tag(name = "App · 支付")
public class AppPayController {

    private final PayChannelService channelService;
    private final PayGateway payGateway;
    private final PayRecordService payRecordService;
    private final OrderService orderService;

    @GetMapping("/channels")
    @Operation(summary = "可用的支付渠道")
    public R<List<Map<String, Object>>> channels() {
        List<Map<String, Object>> payload = channelService.listAvailable().stream()
                .map(view -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", view.code());
                    item.put("name", view.name());
                    item.put("provider", view.provider());
                    item.put("redirect", view.supported() && requiresRedirect(view.provider()));
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
        return R.ok(payload);
    }

    @PostMapping("/orders/{orderNo}")
    @Operation(summary = "发起支付（返回跳转 / 二维码 / SDK 参数）")
    public R<PayLaunch> launch(@PathVariable String orderNo,
                               @RequestBody(required = false) LaunchRequest request,
                               HttpServletRequest http) {
        String channelCode = request == null || request.getChannel() == null
                ? "mock" : request.getChannel();
        PayLaunch launch = orderService.launchPayment(
                LoginUserContext.requiredUserId(), orderNo, channelCode,
                RequestUtils.clientIp(http));
        return R.ok(launch);
    }

    /**
     * Channel callback.
     *
     * <p>The body is read as a raw string and passed through untouched: every
     * provider signs the exact bytes it sent, so letting Spring deserialise it
     * first would break signature verification.
     *
     * <p>Always replies with the channel's own ack format, including on error —
     * a provider that gets an error code retries for up to a day, which for a
     * genuinely bad payload just means a day of noise.
     */
    @PostMapping("/notify/{channel}")
    public ResponseEntity<String> notify(@PathVariable String channel,
                                         @RequestBody(required = false) String body,
                                         HttpServletRequest http) {
        PayGateway.Ack ack = payGateway.ack(channel);
        try {
            PayNotification notification = payGateway.verify(channel, headersOf(http),
                    body == null ? "" : body);

            // Replays are common: providers retry until acknowledged, and some
            // send both a success and a completed event for one payment.
            if (notification.tradeNo() != null
                    && payRecordService.alreadySettled(channel, notification.tradeNo())) {
                log.info("payment callback replayed channel={} tradeNo={}", channel, notification.tradeNo());
                return ResponseEntity.ok().contentType(MediaType.parseMediaType(ack.contentType()))
                        .body(ack.body());
            }

            orderService.settleFromChannel(notification);
        } catch (Exception ex) {
            // Log loudly but still acknowledge: our own bug should be visible in
            // the logs, not multiplied by a day of provider retries.
            log.error("payment callback failed channel={}: {}", channel, ex.getMessage(), ex);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ack.contentType()))
                .body(ack.body());
    }

    /**
     * Browser landing page for redirect flows.
     *
     * <p>Deliberately does not settle the order: it only reports what the
     * channel told us in the query string. Settlement is the callback's job,
     * because the query string is attacker controlled.
     */
    @GetMapping("/return")
    @Operation(summary = "支付完成后的浏览器回跳")
    public R<Map<String, Object>> returned(@RequestParam(required = false) String orderNo,
                                           @RequestParam(required = false) String result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNo", orderNo);
        payload.put("result", result);
        // the client re-reads the order and shows whatever the server says
        payload.put("settled", false);
        return R.ok(payload);
    }

    private static boolean requiresRedirect(String provider) {
        return "stripe".equals(provider) || "alipay".equals(provider);
    }

    private static Map<String, String> headersOf(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    @lombok.Data
    public static class LaunchRequest {
        private String channel;
    }
}
