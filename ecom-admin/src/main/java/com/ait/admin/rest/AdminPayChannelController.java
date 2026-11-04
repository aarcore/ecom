package com.ait.admin.rest;

import com.ait.common.api.R;
import com.ait.logging.annotation.OperationLog;
import com.ait.pay.channel.StripePayChannel;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayChannelRegistry;
import com.ait.pay.service.PayChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Payment channel administration.
 *
 * <p>Two separate concerns share this controller:
 * <ul>
 *   <li>the list and edit endpoints, which drive the console's channel table</li>
 *   <li>{@code /providers}, which tells the console which adapters exist in this
 *       build so it can offer a "add channel" dialog that cannot create a row
 *       nothing can speak to</li>
 * </ul>
 *
 * <p>Secrets never travel out: responses carry {@code hasPrivateKey} style flags
 * and blank on submit means "keep what is stored".
 */
@RestController
@RequestMapping("/api/admin/pay/channels")
@RequiredArgsConstructor
@Tag(name = "Admin · 支付渠道")
public class AdminPayChannelController {

    private final PayChannelService channelService;
    private final PayChannelRegistry registry;

    @GetMapping
    @Operation(summary = "支付渠道列表")
    public R<List<PayChannelService.PayChannelView>> list() {
        return R.ok(channelService.listAll());
    }

    /**
     * Adapters compiled into this build, with the settings each one understands.
     *
     * <p>The {@code settings} hints let the console render the right form fields
     * per provider instead of hardcoding a switch on the provider name.
     */
    @GetMapping("/providers")
    @Operation(summary = "本版本支持的支付渠道类型")
    public R<List<Map<String, Object>>> providers() {
        List<Map<String, Object>> payload = registry.all().stream()
                .map(channel -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", channel.code());
                    item.put("name", channel.label());
                    item.put("redirect", channel.requiresRedirect());
                    item.put("settings", settingHints(channel.code()));
                    return item;
                })
                .collect(java.util.stream.Collectors.toList());
        return R.ok(payload);
    }

    @PostMapping
    @OperationLog(module = "支付渠道", action = "保存支付渠道")
    @Operation(summary = "新增 / 编辑支付渠道")
    public R<PayChannelService.PayChannelView> save(@RequestBody ChannelForm form) {
        return R.ok(channelService.save(form.toInput()));
    }

    @PutMapping("/{code}/enabled")
    @OperationLog(module = "支付渠道", action = "启用/停用支付渠道")
    @Operation(summary = "启用或停用渠道")
    public R<PayChannelService.PayChannelView> toggle(@PathVariable String code,
                                                      @RequestBody ToggleForm form) {
        return R.ok(channelService.setEnabled(code, Boolean.TRUE.equals(form.getEnabled())));
    }

    @PostMapping("/{code}/test")
    @Operation(summary = "检查渠道配置")
    public R<Map<String, Object>> test(@PathVariable String code) {
        Map<String, Object> payload = new LinkedHashMap<>();
        var row = channelService.findEntity(code);
        if (row.isEmpty()) {
            payload.put("ok", false);
            payload.put("message", "渠道不存在");
            return R.ok(payload);
        }
        var adapter = registry.find(code);
        if (adapter.isEmpty()) {
            payload.put("ok", false);
            payload.put("message", "当前版本不支持该渠道");
            return R.ok(payload);
        }
        var config = channelService.resolveIncludingDisabled(code).orElse(null);
        String problem = config == null ? "渠道配置缺失" : adapter.get().checkConfig(config);
        payload.put("ok", problem == null);
        payload.put("message", problem == null ? "配置完整，可以启用" : problem);
        return R.ok(payload);
    }

    @DeleteMapping("/{code}")
    @OperationLog(module = "支付渠道", action = "删除支付渠道")
    @Operation(summary = "删除渠道配置")
    public R<Void> delete(@PathVariable String code) {
        channelService.delete(code);
        return R.ok();
    }

    /**
     * Which non-secret settings a provider reads, so the console can render the
     * right inputs. Kept beside the adapters it describes rather than in the
     * frontend, which would drift as providers are added.
     */
    private List<Map<String, String>> settingHints(String code) {
        return switch (code) {
            case StripePayChannel.CODE -> List.of(
                    hint("webhookSecret", "Webhook 签名密钥", "whsec_ 开头，用于校验回调"));
            case "wechat" -> List.of(
                    hint("merchantSerialNumber", "商户证书序列号", "商户平台 API 安全中查看"));
            case "alipay" -> List.of(
                    hint("gateway", "网关地址", "留空则按模式自动选择沙箱/生产"));
            default -> List.of();
        };
    }

    private static Map<String, String> hint(String key, String label, String description) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key", key);
        map.put("label", label);
        map.put("description", description);
        return map;
    }

    @Data
    public static class ChannelForm {
        private String code;
        private String name;
        private String provider;
        private Boolean enabled;
        private String mode;
        private String appId;
        private String merchantId;
        private String privateKey;
        private String publicKey;
        private String apiKey;
        private String apiSecret;
        private String notifyUrl;
        private String returnUrl;
        private Map<String, String> settings;
        private Integer sort;
        private String remark;

        PayChannelService.PayChannelInput toInput() {
            return new PayChannelService.PayChannelInput(code, name, provider, enabled, mode,
                    appId, merchantId, privateKey, publicKey, apiKey, apiSecret,
                    notifyUrl, returnUrl, settings, sort, remark);
        }
    }

    @Data
    public static class ToggleForm {
        private Boolean enabled;
    }
}
