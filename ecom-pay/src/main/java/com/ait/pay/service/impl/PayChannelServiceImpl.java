package com.ait.pay.service.impl;

import com.ait.common.utils.JsonUtils;
import com.ait.pay.config.ChannelSecretCodec;
import com.ait.pay.core.ChannelConfig;
import com.ait.pay.core.PayChannel;
import com.ait.pay.core.PayChannelRegistry;
import com.ait.pay.core.PayException;
import com.ait.pay.core.PaymentResultCode;
import com.ait.pay.entity.PayChannelEntity;
import com.ait.pay.mapper.PayChannelMapper;
import com.ait.pay.service.PayChannelService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Channel configuration store.
 *
 * <p>Secrets are encrypted on the way in and decrypted on the way out; the
 * entity never leaves this class in either state. The console talks to
 * {@code PayChannelView}, which only says whether a secret is present.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayChannelServiceImpl implements PayChannelService {

    private final PayChannelMapper channelMapper;
    private final PayChannelRegistry registry;
    private final ChannelSecretCodec codec;

    @Override
    public List<PayChannelView> listAll() {
        List<PayChannelEntity> rows = channelMapper.selectList(Wrappers.<PayChannelEntity>lambdaQuery()
                .orderByAsc(PayChannelEntity::getSort)
                .orderByAsc(PayChannelEntity::getId));
        List<PayChannelView> views = new ArrayList<>(rows.size());
        for (PayChannelEntity row : rows) {
            views.add(toView(row, true));
        }
        return views;
    }

    @Override
    public List<PayChannelView> listAvailable() {
        List<PayChannelView> result = new ArrayList<>();
        for (PayChannelView view : listAll()) {
            if (view.enabled() && view.supported() && view.healthMessage() == null) {
                result.add(view);
            }
        }
        return result;
    }

    @Override
    public Optional<ChannelConfig> resolve(String code) {
        if (code == null) {
            return Optional.empty();
        }
        PayChannelEntity row = findRow(code);
        if (row == null || !Integer.valueOf(1).equals(row.getEnabled())) {
            return Optional.empty();
        }
        return Optional.of(toConfig(row));
    }

    @Override
    public Optional<ChannelConfig> resolveIncludingDisabled(String code) {
        PayChannelEntity row = findRow(code);
        return row == null ? Optional.empty() : Optional.of(toConfig(row));
    }

    @Override
    public ChannelConfig requireEnabled(String code) {
        PayChannelEntity row = findRow(code);
        if (row == null) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(row.getEnabled())) {
            throw PayException.channelDisabled(code);
        }
        return toConfig(row);
    }

    @Override
    public Optional<PayChannelEntity> findEntity(String code) {
        return Optional.ofNullable(findRow(code));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayChannelView save(PayChannelInput input) {
        if (input.code() == null || input.code().trim().isEmpty()) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_FOUND, "渠道标识不能为空");
        }
        String code = input.code().trim().toLowerCase();

        // A row may describe a provider this build cannot speak. That is allowed
        // (an operator may pre-stage a channel), but it must be visible as such
        // rather than silently accepted and then failing at checkout.
        PayChannelEntity existing = findRow(code);
        if (existing == null && !registry.exists(code)) {
            log.warn("saving payment channel '{}' with no matching adapter in this build", code);
        }

        PayChannelEntity entity = existing == null ? new PayChannelEntity() : existing;
        entity.setCode(code);
        entity.setName(blankTo(input.name(), code));
        entity.setProvider(blankTo(input.provider(), code));
        entity.setEnabled(input.enabled() == null ? 0 : (input.enabled() ? 1 : 0));
        entity.setMode(blankTo(input.mode(), "demo"));
        entity.setAppId(input.appId());
        entity.setMerchantId(input.merchantId());
        entity.setNotifyUrl(input.notifyUrl());
        entity.setReturnUrl(input.returnUrl());
        entity.setSort(input.sort() == null ? 0 : input.sort());
        entity.setRemark(input.remark());
        entity.setSettingsJson(JsonUtils.toJson(input.settings() == null ? Map.of() : input.settings()));

        // Blank secret means "keep what is stored": the console cannot show the
        // current value, so a form submit without retyping it must not wipe it.
        if (hasText(input.privateKey())) {
            entity.setPrivateKey(codec.encode(input.privateKey()));
        }
        if (hasText(input.publicKey())) {
            entity.setPublicKey(codec.encode(input.publicKey()));
        }
        if (hasText(input.apiKey())) {
            entity.setApiKey(codec.encode(input.apiKey()));
        }
        if (hasText(input.apiSecret())) {
            entity.setApiSecret(codec.encode(input.apiSecret()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            channelMapper.insert(entity);
        } else {
            entity.setUpdatedAt(now);
            channelMapper.updateById(entity);
        }
        return toView(entity, true);
    }

    @Override
    public PayChannelView setEnabled(String code, boolean enabled) {
        PayChannelEntity row = findRow(code);
        if (row == null) {
            throw new PayException(PaymentResultCode.CHANNEL_NOT_FOUND);
        }
        PayChannelEntity patch = new PayChannelEntity();
        patch.setId(row.getId());
        patch.setEnabled(enabled ? 1 : 0);
        patch.setUpdatedAt(LocalDateTime.now());
        channelMapper.updateById(patch);
        row.setEnabled(patch.getEnabled());
        return toView(row, true);
    }

    @Override
    public void delete(String code) {
        PayChannelEntity row = findRow(code);
        if (row == null) {
            return;
        }
        channelMapper.deleteById(row.getId());
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------
    private PayChannelEntity findRow(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return channelMapper.selectOne(Wrappers.<PayChannelEntity>lambdaQuery()
                .eq(PayChannelEntity::getCode, code.trim().toLowerCase())
                .last("limit 1"));
    }

    private ChannelConfig toConfig(PayChannelEntity row) {
        return new ChannelConfig(
                row.getCode(),
                Integer.valueOf(1).equals(row.getEnabled()),
                row.getMode(),
                row.getAppId(),
                row.getMerchantId(),
                codec.decode(row.getPrivateKey()),
                codec.decode(row.getPublicKey()),
                codec.decode(row.getApiKey()),
                codec.decode(row.getApiSecret()),
                row.getNotifyUrl(),
                row.getReturnUrl(),
                parseSettings(row.getSettingsJson()));
    }

    private PayChannelView toView(PayChannelEntity row, boolean withHealth) {
        Optional<PayChannel> adapter = registry.find(row.getCode());
        String health = null;
        if (withHealth && adapter.isPresent()) {
            try {
                health = adapter.get().checkConfig(toConfig(row));
            } catch (Exception ex) {
                // a missing credential must not break the whole channel list
                health = ex.getMessage();
            }
        }
        return new PayChannelView(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getProvider(),
                Integer.valueOf(1).equals(row.getEnabled()),
                row.getMode(),
                row.getAppId(),
                row.getMerchantId(),
                hasText(row.getPrivateKey()),
                hasText(row.getPublicKey()),
                hasText(row.getApiKey()),
                hasText(row.getApiSecret()),
                row.getNotifyUrl(),
                row.getReturnUrl(),
                parseSettings(row.getSettingsJson()),
                row.getSort(),
                row.getRemark(),
                adapter.isPresent(),
                health);
    }

    private Map<String, String> parseSettings(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, String> parsed = JsonUtils.parse(json,
                new TypeReference<LinkedHashMap<String, String>>() {
                }, null);
        return parsed == null ? new LinkedHashMap<>() : parsed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
