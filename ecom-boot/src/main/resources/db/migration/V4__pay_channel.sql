-- =====================================================================
--  Payment channels
--
--  One row per configured provider. Rows exist for channels that are not
--  switched on: the console lists every provider the build supports, an
--  operator fills in credentials and flips `enabled`.
--
--  Credentials are stored encrypted (AES-GCM, see ChannelSecretCodec) because
--  a database dump or a read replica would otherwise hand out a live Stripe
--  secret key. The column names stay generic (*_key / *_secret) so the same
--  table serves every provider.
-- =====================================================================
create table shop_pay_channel
(
    id             bigserial primary key,
    code           varchar(32)   not null, -- matches PayChannel.code(), e.g. stripe
    name           varchar(64)   not null,
    provider       varchar(32)   not null, -- stripe | wechat | alipay | mock
    enabled        smallint      not null default 0,
    mode           varchar(16)   not null default 'demo', -- demo | test | live
    app_id         varchar(128),
    merchant_id    varchar(128),
    private_key    text,                   -- ciphertext
    public_key     text,                   -- ciphertext
    api_key        text,                   -- ciphertext
    api_secret     text,                   -- ciphertext
    notify_url     varchar(500),
    return_url     varchar(500),
    settings_json  text,                   -- provider specific, non-secret
    sort           integer       not null default 0,
    remark         varchar(500),
    created_at     timestamp     not null default now(),
    updated_at     timestamp     not null default now()
);

create unique index uk_pay_channel_code on shop_pay_channel (code);

-- ---------------------------------------------------------------------
--  Extend the payment log so a record can be tied back to a channel
--  callback and reconciled against the provider dashboard.
-- ---------------------------------------------------------------------
alter table shop_pay_record
    add column channel_code  varchar(32),
    add column out_trade_no  varchar(64),
    add column currency      varchar(8) not null default 'CNY',
    add column raw_status    varchar(64),
    add column notify_at     timestamp,
    add column fail_reason   varchar(500);

-- a channel transaction id must be unique per channel, otherwise a replayed
-- callback could settle a second order
create unique index uk_pay_record_channel_trade
    on shop_pay_record (channel_code, trade_no)
    where channel_code is not null and trade_no is not null;

create index idx_pay_record_channel on shop_pay_record (channel_code, created_at desc);

-- ---------------------------------------------------------------------
--  Seed
--
--  The mock channel ships enabled so a fresh clone can complete a purchase
--  with no configuration. The three real providers ship disabled with their
--  non-secret settings pre-filled, so an operator only pastes credentials.
-- ---------------------------------------------------------------------
insert into shop_pay_channel (code, name, provider, enabled, mode, sort, settings_json, remark) values
  ('mock', '模拟支付', 'mock', 1, 'demo', 100,
   '{}',
   '演示渠道：下单即完成支付，无需任何配置'),

  ('stripe', 'Stripe', 'stripe', 0, 'test', 90,
   '{"webhookSecret":""}',
   'Stripe Checkout（跳转支付）。填入 Secret Key 与 Webhook 签名密钥后启用'),

  ('alipay', '支付宝', 'alipay', 0, 'test', 80,
   '{"gateway":"https://openapi-sandbox.dl.alipaydev.com/gateway.do"}',
   '支付宝电脑网站支付。填入应用 AppID、应用私钥与支付宝公钥后启用'),

  ('wechat', '微信支付', 'wechat', 0, 'test', 70,
   '{"merchantSerialNumber":""}',
   '微信支付 APIv3 Native 扫码。填入 AppID、商户号、商户私钥、APIv3 密钥与证书序列号后启用');
