package com.ait.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * One configured payment channel.
 *
 * <p>Rows exist for channels that are not switched on: the console lists every
 * provider this build supports, an operator fills in credentials and flips
 * {@code enabled}. Deleting a row is therefore not how a channel is disabled —
 * {@code enabled} is.
 *
 * <p>Secrets are encrypted at rest by {@code ChannelConfigCodec} before they
 * reach this entity, so the {@code *Key} / {@code *Secret} columns hold
 * ciphertext. Reads via the admin API never expose them.
 */
@Data
@TableName("shop_pay_channel")
public class PayChannelEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Matches {@code PayChannel#code()}. Unique. */
    private String code;

    private String name;

    /** The provider family, e.g. stripe / wechat / alipay / mock. */
    private String provider;

    /** 1 on / 0 off. A channel that is off disappears from the storefront. */
    private Integer enabled;

    /** demo / test / live — used by adapters to pick sandbox endpoints. */
    private String mode;

    private String appId;

    private String merchantId;

    /** Ciphertext. */
    private String privateKey;

    /** Ciphertext. */
    private String publicKey;

    /** Ciphertext. */
    private String apiKey;

    /** Ciphertext. */
    private String apiSecret;

    private String notifyUrl;

    private String returnUrl;

    /** JSON object of provider specific, non-secret settings. */
    private String settingsJson;

    private Integer sort;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
