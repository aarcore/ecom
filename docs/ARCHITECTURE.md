# 系统架构说明

本文说明 Ecom 的整体结构、模块职责、关键链路与设计取舍。面向需要改动代码或排查问题的开发者。

- [1. 全局视图](#1-全局视图)
- [2. 模块划分](#2-模块划分)
- [3. 依赖方向](#3-依赖方向)
- [4. 请求链路](#4-请求链路)
- [5. 关键设计](#5-关键设计)
- [6. 数据模型](#6-数据模型)
- [7. 支付子系统](#7-支付子系统)
- [8. 多语言子系统](#8-多语言子系统)
- [9. 前端架构](#9-前端架构)
- [10. 部署拓扑](#10-部署拓扑)
- [11. 已知约束](#11-已知约束)

---

## 1. 全局视图

一套 Spring Boot 单体服务，同时为三个前端提供 API。没有独立认证服务、没有消息队列、不拆微服务。

```
                    ┌──────────────────────────────────────────┐
                    │           浏览器 / 小程序宿主             │
                    └───────┬──────────────┬─────────────┬─────┘
                            │              │             │
                 ┌──────────▼───┐ ┌────────▼─────┐ ┌─────▼────────┐
                 │ admin-web    │ │  app-web     │ │  app-mini    │
                 │ React + AntD │ │ React (PC)   │ │ React (H5)   │
                 │ nginx:80     │ │ nginx:80     │ │ nginx:80     │
                 └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
                        │  /api 反向代理 │               │
                        └────────────────┼───────────────┘
                                         │
                          ┌──────────────▼───────────────┐
                          │        ecom-boot             │
                          │  Spring Boot 2.7 · JDK 17    │
                          │  /api/admin/**  /api/app/**  │
                          │         :8080                │
                          └───┬──────────┬───────────┬───┘
                              │          │           │
                    ┌─────────▼──┐  ┌────▼─────┐  ┌──▼──────────────┐
                    │ PostgreSQL │  │  Redis 7 │  │ 第三方支付网关   │
                    │     16     │  │          │  │ Stripe/微信/支付宝│
                    └────────────┘  └──────────┘  └─────────────────┘
```

三个前端都是纯静态资源，构建产物由 nginx 直接托管，`/api`、`/images`、`/uploads` 反向代理到后端。因此三个站与后端同源，没有跨域问题（开发期由 Vite 代理承担同样职责）。

---

## 2. 模块划分

```
ecom/
├── ecom-common/            横切基础：响应体、错误码、异常、JWT、CORS、多语言协商
├── ecom-mbp/               MyBatis-Plus 装配、BaseEntity、@Query 条件构造
├── ecom-kits/
│   ├── ecom-kits-redis/    StringRedisTemplate 门面
│   └── ecom-kits-oss/      存储抽象：本地磁盘 / S3 兼容
├── ecom-mall/
│   ├── ecom-mall-common/   跨端共享枚举与 DTO（PayType、OrderStatus、OrderItemRequest）
│   └── ecom-mall-core/     全部业务实现：实体、Mapper、Service、支付对接
├── ecom-pay/               支付网关：PayChannel 抽象 + 四家渠道实现
├── ecom-logging/           @OperationLog 切面 → sys_operation_log
├── ecom-admin/             /api/admin/** 控制器
├── ecom-app/               /api/app/** 控制器 + 定时任务
├── ecom-boot/              唯一启动类、Flyway 迁移、静态演示素材
├── ecom-generator/         开发辅助：按表生成实体骨架（不参与运行）
├── ecom-admin-web/         运营后台（React + Ant Design）
├── ecom-app-web/           PC 商城（React，手写 CSS）
└── ecom-app-mini/          H5 / 小程序（React，手写 CSS）
```

**分层约定**：`ecom-admin` 与 `ecom-app` 只做参数接收与响应包装，不含业务判断。所有业务逻辑在 `ecom-mall-core` 内，因此后台的"发货"与将来的自动化发货可以调用同一个方法。

---

## 3. 依赖方向

依赖严格单向，没有循环：

```
ecom-boot ──▶ ecom-admin ──┐
         ──▶ ecom-app   ──┼──▶ ecom-mall-core ──▶ ecom-pay ──▶ ecom-common
                          │         │                            ▲
                          │         ├──▶ ecom-mall-common        │
                          │         └──▶ ecom-mbp / kits ────────┘
                          └──▶ ecom-logging
```

两点值得注意：

**`ecom-pay` 不反向依赖商城。** 支付模块只知道 `PayRequest`（订单号、金额、币种），不知道 `Order` 实体。回调结算由商城实现 `PaymentListener` 反注册进来。因此支付能力可以整体抽走复用到别的项目。

**`ecom-mall-core` 依赖 `ecom-pay`，而非相反。** `OrderService.launchPayment` 调用 `PayGateway`；回调时 `MallPaymentListener` 再调回 `OrderService.settleFromChannel`。这构成一个真实的循环引用，用 `ObjectProvider` 延迟注入打破（见 `MallPaymentListener`）。

---

## 4. 请求链路

以"下单并支付"为例，展示一次完整穿越：

```
1  POST /api/app/orders
   ├─ JwtAuthInterceptor    校验 app audience token
   ├─ LocaleFilter          协商语言，写入 LocaleContext
   ├─ OrderController       组装 OrderCreateRequest
   └─ OrderServiceImpl.create
        ├─ 校验地址归属、券归属
        ├─ CouponCalculator   算优惠（与预览共用同一实现）
        ├─ UPDATE sku SET stock = stock - n WHERE stock >= n    ← 条件更新防超卖
        ├─ INSERT shop_order / shop_order_item                  ← 快照价格与规格
        └─ 清空已购购物车行

2  POST /api/app/pay/orders/{orderNo}
   └─ OrderServiceImpl.launchPayment
        ├─ PayChannelService.resolveIncludingDisabled   读 shop_pay_channel
        ├─ PayGateway.create → channel.checkConfig → 渠道下单
        ├─ PayRecordService.recordAttempt                写 pending 流水
        └─ 返回 mode = REDIRECT | QR_CODE | SDK | SYNC

3  渠道异步回调 POST /api/app/pay/notify/{channel}
   ├─ JwtAuthInterceptor    该路径在白名单内（用签名认证）
   ├─ PayGateway.verify     各渠道自行验签 / 解密
   ├─ 幂等检查              alreadySettled(channel, tradeNo)
   └─ OrderServiceImpl.settleFromChannel
        ├─ 金额比对（compareTo，防篡改）
        ├─ PayRecordService.markPaid  条件更新 status 0 → 1
        ├─ UPDATE shop_order SET status = 待发货 WHERE status = 待付款
        ├─ 累计会员消费、核销优惠券
        └─ 返回渠道要求的 ack 格式
```

**浏览器回跳（`/api/app/pay/return`）不结算订单。** 该 URL 由用户控制，`?result=success` 可伪造；只有验签过的回调才改变订单状态。

---

## 5. 关键设计

### 5.1 双 audience JWT

同一套签发逻辑，两种 audience：`/api/admin/**` 要 `admin`，`/api/app/**` 要 `app`。校验发生在 MVC 拦截器而非 Security 过滤链，原因是小程序常把 token 放在 query 参数而非 header。

白名单按 audience 分别配置在 `AppWebConfig` / `AdminWebConfig`：商品浏览、首页聚合、登录发码、支付回调对 C 端开放；后台只有登录接口开放。

### 5.2 库存不超卖

```sql
update shop_product_sku
   set stock = stock - :qty, sales = sales + :qty
 where id = :skuId and stock >= :qty
```

影响行数为 0 即库存不足，抛异常回滚整个事务。避开"先查再写"的竞态窗口。

### 5.3 预览与下单同源

`POST /orders/preview` 与 `POST /orders` 都调用 `CouponCalculator`，输入相同的商品行与券，因此预览金额必然等于实付金额。券的自动择优逻辑也在这里，客户端传 `couponOptOut: true` 可关闭自动选券。

### 5.4 订单状态机

```
        ┌──────────── 退款 ────────────┐
        │                              ▼
  待付款 ─支付→ 待发货 ─发货→ 待收货 ─确认→ 已完成
     │            │              │
   取消/超时      退款           退款
     ▼            ▼              ▼
  已取消       已退款          已退款
```

`待付款` → `待发货` 的迁移使用带条件的 UPDATE（`where status = 0`），因此重复支付回调只会成功一次，不会重复发放积分或重复核销券。

### 5.5 定时任务

`OrderMaintenanceJob` 两个任务，均幂等：

| 任务 | 频率 | 行为 |
| --- | --- | --- |
| 关闭超时未付订单 | 5 分钟 | 超过 45 分钟未支付 → 取消、归还库存、释放券 |
| 自动确认收货 | 1 小时 | 发货满 7 天 → 置为已完成 |

频率可通过 `ecom.jobs.*` 调整。多实例部署下会产生重复的 no-op 日志，但不会重复执行。

### 5.6 审计日志

`@OperationLog(module, action)` 标注在管理端写接口上，AOP 切面记录操作人、动作、入参与耗时到 `sys_operation_log`。它只写不读业务数据，因此不会影响主链路事务。

---

## 6. 数据模型

19 张业务表 + 1 个报表视图，按职责分组：

| 分组 | 表 | 说明 |
| --- | --- | --- |
| 运营 | `sys_admin`、`sys_operation_log` | 后台账号与审计 |
| 会员 | `shop_member`、`shop_member_address` | 账号、地址簿 |
| 商品 | `shop_category`、`shop_brand`、`shop_product`、`shop_product_sku` | 两级分类、SPU/SKU |
| 交易 | `shop_cart`、`shop_order`、`shop_order_item`、`shop_pay_record` | 购物车、订单、支付流水 |
| 营销 | `shop_coupon`、`shop_member_coupon` | 券模板与持券 |
| 运营位 | `shop_banner`、`shop_notice` | 轮播、公告 |
| 支付 | `shop_pay_channel` | 渠道配置（凭据加密） |
| 多语言 | `shop_i18n_text`、`shop_i18n_term` | 译文覆盖表 |

统一约定：

- 金额 `numeric(12,2)`，不用浮点
- 软删除统一 `deleted smallint`（0 正常 / 1 删除），MyBatis-Plus 逻辑删除自动过滤
- 主键 `bigserial`，时间 `timestamp`
- 商品价格与库存在 `shop_product` 上是冗余聚合，实际扣减发生在 `shop_product_sku`

**聚合字段的一致性**：`shop_product.stock/sales` 由 `refreshAggregates` 从 SKU 汇总，写入时同步更新。这是为了让列表页不必 JOIN SKU 表。

---

## 7. 支付子系统

```
                       ┌──────────────────┐
   OrderServiceImpl ──▶│    PayGateway    │  编排：解析渠道、校验配置、统一报错
                       └────────┬─────────┘
                                │
                    ┌───────────▼───────────┐
                    │  PayChannelRegistry   │  启动时收集所有 PayChannel 实现
                    └───────────┬───────────┘
                                │
        ┌───────────┬───────────┼───────────┬────────────┐
        ▼           ▼           ▼           ▼
   StripePay    WechatPay   AlipayPay   MockPay      （未来新增）
   Checkout     Native 扫码  电脑网站支付  同步完成
```

**渠道配置在数据库，不在配置文件。** `shop_pay_channel` 一行即一个渠道：凭据、模式（demo/test/live）、回调地址、启停开关。运营在后台填密钥后立即生效，无需重启。

**新增渠道的完整步骤：**

1. 实现 `PayChannel` 接口，标注 `@Component`
2. 插入一行 `shop_pay_channel`
3. 后台"检测配置"验证凭据完整

订单流程一行都不用改。

**凭据加密**：私钥与密钥经 AES-GCM 加密后写库（`ChannelSecretCodec`），主密钥来自 `ECOM_PAY_SECRET`。管理端接口只返回"是否已配置"的布尔值，从不回显明文；表单留空表示保持原值。这样一次数据库导出不会泄露可用的 Stripe 密钥。

**三家渠道的差异被适配层吸收：**

| | Stripe | 微信支付 | 支付宝 |
| --- | --- | --- | --- |
| 支付形态 | Checkout 跳转 | Native 扫码 | 返回自动提交的表单 |
| 金额单位 | 最小货币单位（分） | 分 | 元（字符串） |
| 回调形态 | 签名 JSON | AES-GCM 加密 + RSA 验签 | 表单编码 + RSA2 验签 |
| 成功标志 | `checkout.session.completed` | `trade_state = SUCCESS` | `TRADE_SUCCESS` / `TRADE_FINISHED` |
| 应答格式 | `{}` | `{"code":"SUCCESS"}` | 字面量 `success` |

**幂等的三层保障：**

1. 内存预检 `alreadySettled(channel, tradeNo)`
2. `shop_pay_record` 的条件更新（`where status = 0`）
3. 唯一索引 `(channel_code, trade_no)`

金额比对用 `BigDecimal.compareTo` —— `10.0` 与 `10.00` 是同一笔钱，但 `equals` 判不等。

---

## 8. 多语言子系统

分两层，界面文案与商品内容各自管理：

**界面文案在前端。** `ecom-app-web/src/i18n/messages.ts` 是唯一字典。中文是源语言的形状，英/日声明为 `Record<MessageKey, string>`，少一条即编译失败。切换语言只重渲染，不发请求。

**商品内容在后端。** 分类名、商品名/副标题/详情、品牌描述、券名、轮播标题、公告来自数据库，通过两张覆盖表表达：

- `shop_i18n_text` — `(entity, entity_id, field, lang)` 定位长文本
- `shop_i18n_term` — 以中文字符串自身为键，用于规格名、规格值、单位这类封闭词表

**中文永远留在原表**，覆盖表只存其他语言。因此：重置演示数据不依赖翻译表；缺译自然回退中文；`?lang=zh-CN` 的请求零额外查询。

**语言协商顺序**：`?lang=` 参数 → `X-Lang` 头 → `Accept-Language`（按 q 值取最优）。前端非中文时给 GET 加 `?lang=`，因为商品接口可缓存，只发请求头会让浏览器复用之前的中文响应。

**订单行不冻结译文**：`shop_order_item.product_name` 始终是中文快照，展示时按 `product_id` 查译文。这样用户切换语言后历史订单跟着变。

译文由 `scripts/gen_i18n_sql.py` 生成成 Flyway 迁移，随 schema 一起版本化。

---

## 9. 前端架构

三个独立 Vite 应用，没有共享组件库（刻意如此：C 端手写 CSS 控制包体积，后台用 Ant Design 换取开发效率）。

| 应用 | 路由/状态 | 样式 | 特点 |
| --- | --- | --- | --- |
| `ecom-admin-web` | react-router + zustand | Ant Design 5 + 少量全局 CSS | 表格为主，52px 顶栏 / 208px 侧栏 |
| `ecom-app-web` | react-router + zustand | 手写 CSS，语义化 token | 双主题、三语言、响应式 |
| `ecom-app-mini` | react-router + zustand | 手写 CSS，移动优先 | 可直接嵌入小程序 webview |

**PC 商城的主题与图片**：`tokens.css` 定义语义 token（`--canvas/--surface/--page/--plate`），浅深两套。照片按目录约定切换亮色版本（`/images/products/x.jpg` → `/images/products/light/x.jpg`），由 `Photo.tsx` 统一处理，缺图自动回退。

**API 客户端**：三端各有一份 `api/client.ts`，负责注入 token、统一解包 `{code,message,data}`、401 时广播事件让外壳跳转登录。C 端额外注入 `X-Lang` 与 `?lang=`。

---

## 10. 部署拓扑

`docker-compose.yml` 六个服务：

| 服务 | 镜像 | 端口 | 说明 |
| --- | --- | --- | --- |
| `backend` | `ecom/backend` | 8080 | 多阶段构建，依赖层独立缓存 |
| `admin-web` | `ecom/admin-web` | 80 | 静态包 |
| `app-web` | `ecom/app-web` | 80 | 静态包 |
| `app-mini` | `ecom/app-mini` | 80 | 静态包 |
| `postgres` | `postgres:16-alpine` | 5432 | 数据卷 `postgres-data` |
| `redis` | `redis:7-alpine` | 6379 | 数据卷 `redis-data` |

宿主机端口由 `.env` 控制（仓库默认 `18080`–`18083` / `15432` / `16379`，避免与本机其他容器冲突）。

**启动顺序**：postgres/redis 有 healthcheck，backend 通过 `depends_on: condition: service_healthy` 等待就绪；Flyway 在启动时自动执行迁移。

**静态素材**：`/images/**` 由后端 jar 内的 `static/` 提供，nginx 反代；`/uploads/**` 为运行时上传，落在 `ecom-uploads` 卷上，重建容器不丢。

---

## 11. 已知约束

诚实记录当前的取舍，避免误以为是疏漏：

**单库单表无分片。** 订单表按时间增长，没有分区也没有归档策略。演示规模没问题，真实业务量下需要按 `created_at` 分区。

**`@Scheduled` 无分布式锁。** 多实例下任务会各跑一遍。业务操作幂等所以不出错，但日志会重复；如需严格单实例执行，可加 Redis 锁（`RedisService` 已有 `setIfAbsent` 能力可用）。

**支付回调没有重试队列。** 验签失败或处理异常时记录日志并返回 ack，依赖渠道侧的重推。没有本地补偿表，极端情况下需要人工对账。

**小程序登录是模拟实现。** 未配置 `WECHAT_APP_ID` 时从 code 派生稳定的伪 openid，仅用于演示。接入真实 `jscode2session` 需替换 `MemberServiceImpl` 中的分支。

**短信验证码回显。** `AppAuthController#sendSms` 把验证码放在响应的 `demoCode` 字段里，便于无短信网关时测试。上线前必须删除。

**没有接口级限流。** 仅短信发送有 60 秒频率限制（Redis），其余接口无限流。

**登出不使 token 失效。** JWT 是无状态的，`/logout` 只清客户端存储与上下文；`CommonConstants` 里预留了黑名单 key 前缀，但没有写入逻辑。因此在 token 过期前（默认 7 天），被登出的 token 仍然可用。若需真正失效，应在登出时把 `jti` 写入 Redis 并在拦截器校验。

**事务边界在 Service 方法上。** 跨模块调用（如支付回调 → 订单结算）在同一事务内，若第三方 SDK 在事务中发起网络调用会拉长事务；当前支付渠道调用都在事务外，回调处理在事务内但仍包含一次数据库往返。
