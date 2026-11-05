<div align="center">

# Ecom

可插拔支付网关的单体电商系统，可用作跨境独立站

桌面前台 · 移动 H5 · 运营后台 · Stripe / 微信支付 / 支付宝

[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-orange.svg)](https://github.com/aarcore/ecom/releases)
[![Java](https://img.shields.io/badge/runtime-JDK%2017-blue.svg)](https://adoptium.net/)

[![Spring Boot](https://img.shields.io/badge/spring%20boot-2.7.13-6db33f.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/database-PostgreSQL%2016-4169e1.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/cache-Redis%207-dc382d.svg)](https://redis.io/)
[![Stack](https://img.shields.io/badge/stack-Spring%20%7C%20React%20%7C%20TypeScript-000000.svg)](#技术栈)

</div>

---

Ecom 是一个 Spring Boot 服务，同时支撑三套前端。顾客在桌面前台挑车，在能被小程序 webview 直接加载的 H5 页面上下单，运营在同一批订单上发货。整套系统是一个可部署的 jar 加每个客户端一份静态包，因此本地跑通的下单链路与线上走的是同一份代码。

真正花心思的是支付层。渠道是数据库里的一行记录，不是配置文件：运营在后台粘贴 Stripe 密钥、打开开关，下次加载页面商城就开始提供这个渠道——不用重新部署，不用重启。新增一家支付只需要一个类加一行数据。

没有独立的认证服务，没有消息队列，也不拆微服务。状态放在 PostgreSQL，可重建的放 Redis，过期订单交给 `@Scheduled` 关闭。

### 可作为跨境独立站的基础

这套工程的架构天然适合做**跨境独立站**（D2C 品牌站），原因不是它做了很多跨境功能，而是它没有把「只做中国」写死在结构里：

| 跨境需要的能力 | 本工程现状 |
| --- | --- |
| 多语言前台 | ✅ 中文 / English / 日本語，界面文案与商品内容都能切换，可继续加语种 |
| 海外收款 | ✅ 内置 Stripe Checkout，支持任意 Stripe 支持的币种；微信/支付宝作为国内渠道并存 |
| 无平台依赖 | ✅ 自有域名、自有前端、自有订单数据，不经任何平台抽佣或审核 |
| 单机可跑 | ✅ 一个 jar + 静态包 + PostgreSQL，VPS 即可部署 |
| 独立品牌 | ✅ 前台样式、图片、文案全部可替换，无第三方水印或强制页脚 |
| 支付凭据隔离 | ✅ 渠道凭据加密落库，可由运营在后台轮换，无需改代码发布 |

**接入海外业务时需要补的部分**（代码里目前没有，属于业务扩展而非架构障碍）：

- **币种**：金额在库中只有 `numeric(12,2)` 一个数值，没有币种字段；前端统一按 CNY 格式化。做多币种需给 `shop_product` / `shop_order` 加 `currency`，并让 `formatPrice` 使用订单币种。
- **地址**：地址簿是「省 / 市 / 区 / 详细」，没有国家字段，也没有邮编。海外收货需要改成国家 + 州/省 + 邮编的结构。
- **税费**：没有 VAT / GST / 销售税计算，价格即含税价。
- **物流**：发货只记录快递公司与运单号，没有承运商 API 集成或面单。
- **合规**：没有 GDPR 数据导出/删除入口，也没有 Cookie 同意条。

所以更准确的说法是：**这是一个可直接改为跨境独立站的电商内核**——语言、支付、部署、数据归属都已经就位，剩下的是币种、地址、税费这类业务字段的扩展。

---

## 目录

- [快速开始（Docker）](#快速开始docker)
- [可作为跨境独立站的基础](#可作为跨境独立站的基础)
- [本地开发](#本地开发)
- [模块结构](#模块结构)
- [关键设计](#关键设计)
- [数据库与迁移](#数据库与迁移)
- [功能清单](#功能清单)
- [测试](#测试)
- [文档索引](#文档索引)
- [生产部署注意事项](#生产部署注意事项)
- [技术栈](#技术栈)
- [License](#license)

---

## 快速开始（Docker）


```bash
cp .env.example .env       # 可选：脚本会自动创建
./scripts/deploy.sh        # 或 docker compose up -d --build
```

| 服务 | 默认地址 | 说明 |
| --- | --- | --- |
| 运营后台 | http://localhost:8081 | React + Ant Design 控制台 |
| PC 商城 | http://localhost:8083 | 桌面优先布局，自动适配平板与手机 |
| H5 / 小程序 | http://localhost:8082 | 移动优先 H5，可直接嵌入小程序 webview |
| 接口文档 | http://localhost:8080/swagger-ui.html | OpenAPI，按 admin / app 分组 |
| PostgreSQL | `localhost:5432` | 库名 `ecom`，Flyway 自动建表 + 初始化 |
| Redis | `localhost:6379` | 验证码、限流、Token 黑名单 |

**演示账号**

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 运营管理员 | `admin` | `admin123` |
| 商城会员 | `13800138000` | `123456` |

首次启动会自动执行四个迁移脚本，写入 24 个商品、121 个 SKU、18 位会员、近 30 天的 90 笔订单与英日双语译文，因此看板与列表页开箱即有数据。

> 端口被占用时，在 `.env` 中修改 `API_PORT` / `ADMIN_UI_PORT` / `APP_MINI_PORT` / `APP_WEB_PORT` / `DB_PORT` / `REDIS_PORT` 即可。仓库自带的 `.env` 已经改用 `18080`–`18083` / `15432` / `16379`，避免与本机其他容器冲突。

### 常用命令

```bash
docker compose ps                 # 查看状态
docker compose logs -f backend    # 跟踪后端日志
docker compose down               # 停止，保留数据卷
docker compose down -v            # 停止并清空数据（重建演示库）
./scripts/deploy.sh --rebuild     # 强制重建镜像
./scripts/deploy.sh --reset       # 清库 + 重建 + 启动
```

---

## 本地开发

```bash
./scripts/dev.sh      # 仅启动 PostgreSQL + Redis
```

> 脚本默认使用 compose 里的端口。若 `.env` 改过端口（本仓库就是），需要相应传入
> `DB_PORT=15432 REDIS_PORT=16379`。

然后分别启动四个进程：

```bash
# 后端（JDK 17）
export DB_HOST=localhost DB_PORT=15432 DB_NAME=ecom DB_USER=postgres DB_PASSWORD=postgres
export REDIS_HOST=localhost REDIS_PORT=16379
mvn -pl ecom-boot -am spring-boot:run -Dspring-boot.run.profiles=dev

# 运营后台 http://localhost:5173
cd ecom-admin-web && npm install && npm run dev

# PC 商城 http://localhost:5175
cd ecom-app-web && npm install && npm run dev

# H5 / 小程序 http://localhost:5174
cd ecom-app-mini && npm install && npm run dev
```

三个 Vite 开发服务器都会把 `/api`、`/images`、`/uploads` 代理到 `http://localhost:8080`。
后端端口不同时，用 `VITE_PROXY_TARGET=http://localhost:18080 npm run dev` 覆盖。

### 只想改前端？

改完前端不必重建镜像。构建产物拷进正在运行的容器即可，几秒钟完成：

```bash
cd ecom-admin-web && npm run build
docker cp dist/. ecom-admin-web-id:/usr/share/nginx/html/   # 或 ecom-app-web / ecom-app-mini
```

后端同理，把 `ecom-boot/target/ecom-server.jar` 拷进容器后 `docker restart ecom-backend`。

---

## 模块结构

```
┌───────────────┐ ┌───────────────┐ ┌──────────────────┐
│ecom-admin-web │ │ ecom-app-web  │ │  ecom-app-mini   │
│React + AntD   │ │  React (PC)   │ │ React (H5/小程序) │
└──────┬────────┘ └──────┬────────┘ └────────┬─────────┘
       │  /api           │  /api            │  /api
       └─────────────────┼──────────────────┘
                         ▼
        ┌────────────────────────────────┐
        │      ecom-boot（单体入口）      │  Spring Boot 2.7 / JDK 17
        │   /api/admin/**  /api/app/**   │
        └───┬──────────┬─────────────┬───┘
            │          │             │
      ┌─────▼────┐ ┌───▼─────┐ ┌─────▼──────────┐
      │PostgreSQL│ │  Redis  │ │  ecom-pay      │
      │  (数据)   │ │(缓存/限流)│ │ Stripe/微信/支付宝│
      └──────────┘ └─────────┘ └────────────────┘
```

```
ecom/
├── ecom-common/           统一响应体、错误码、异常处理、JWT、CORS、多语言协商
├── ecom-mbp/              MyBatis-Plus 配置、通用实体/分页/查询构造器
├── ecom-kits/
│   ├── ecom-kits-redis/   Redis 序列化与 RedisService 门面
│   └── ecom-kits-oss/     存储抽象（本地磁盘 / S3 兼容）
├── ecom-mall/
│   ├── ecom-mall-common/  跨端共享枚举与 DTO
│   └── ecom-mall-core/    实体、Mapper、Service（订单/购物车/会员/商品/多语言）
├── ecom-pay/              支付网关：渠道抽象 + Stripe / 微信 / 支付宝 / 模拟
├── ecom-logging/          @OperationLog 切面，写入审计表
├── ecom-admin/            /api/admin/** 控制器
├── ecom-app/              /api/app/** 控制器 + 定时任务
├── ecom-boot/             唯一可执行入口、Flyway 迁移脚本、静态演示素材
├── ecom-generator/        MyBatis-Plus 代码生成器（开发辅助）
├── ecom-admin-web/        React 运营后台
├── ecom-app-web/          React PC Web 商城（响应式，向下适配移动端）
├── ecom-app-mini/         React C 端（H5 + 小程序 webview）
├── docker/                后端镜像 Dockerfile
├── docker-compose.yml     全栈编排
└── scripts/               部署、开发、冒烟测试、素材生成脚本
```

`ecom-admin` 与 `ecom-app` 是纯粹的控制器层，业务逻辑全部沉在 `ecom-mall-core`，因此同一个下单/发货服务可以被后台与小程序复用。`ecom-pay` 只依赖 `ecom-common`，不反向依赖商城，所以支付能力可以单独复用。

---

## 关键设计

### 1. 双端鉴权，Token 不互通

同一套 JWT 实现签发两种 audience：

| 模块 | audience | 白名单 |
| --- | --- | --- |
| `/api/admin/**` | `admin` | 仅 `/login` |
| `/api/app/**` | `app` | 首页、商品、分类、登录、发码、支付回调 |

`JwtAuthInterceptor` 在 MVC 层校验，而不是 Spring Security 过滤器链，原因是**小程序常把 token 放在 query 参数**而非 header。把 C 端 token 拿去访问 `/api/admin/**` 会被拒绝（冒烟测试覆盖了这一点）。

### 2. 库存不超卖

下单时对每个 SKU 执行带条件的原子更新：

```sql
update shop_product_sku
   set stock = stock - :qty, sales = sales + :qty
 where id = :skuId and stock >= :qty
```

影响行数为 0 即代表库存不足，直接抛业务异常回滚整个事务。避免了「先查再写」在并发下的经典超卖问题。

### 3. 价格与优惠一致

结算预览 (`/orders/preview`) 与真正下单 (`POST /orders`) 共用 `CouponCalculator`：

- `amount` 券：固定立减，上限为商品金额
- `discount` 券：8.5 折 = 应付 85%
- 未指定券时服务端自动挑选**优惠最大**的一张
- 客户端选择「不使用优惠券」时传 `couponOptOut: true`，服务端不再自动套用

预览金额与实付金额因此始终一致（冒烟测试校验了 ¥7998 − ¥20 = ¥7978）。

### 4. 订单状态机

```
待付款 ──支付──▶ 待发货 ──发货──▶ 待收货 ──确认──▶ 已完成
   │                 │                  │
  取消/超时         退款               退款
   ▼                 ▼                  ▼
 已取消           已退款             已退款
```

- 每 5 分钟扫描超过 45 分钟未支付的订单并自动关闭、归还库存、释放优惠券
- 每小时将发货超过 7 天的订单自动确认收货
- 两个任务都是幂等的，多实例部署安全

### 5. 支付网关

`ecom-pay` 把「渠道」抽象为 `PayChannel` 接口，订单流程只与 `PayGateway` 对话：

```
OrderServiceImpl ──▶ PayGateway ──▶ PayChannelRegistry ──▶ PayChannel
                         │                                   ├── StripePayChannel
                         ▼                                   ├── WechatPayChannel
                  PayChannelService（读写配置）                ├── AlipayPayChannel
                  PayRecordService（幂等流水）                 └── MockPayChannel
```

**渠道由数据库驱动，不是配置文件。** 运营在后台填密钥、启停渠道、检测配置，全程无需重启。新增一家支付只需两步：实现 `PayChannel` 并标注 `@Component`，插入一行 `shop_pay_channel`。订单流程一行不改。

几种渠道差异被适配层吃掉，上层只看到统一结果：

| | Stripe | 微信支付 | 支付宝 |
| --- | --- | --- | --- |
| 支付形态 | Checkout 跳转 | Native 扫码 | 电脑网站支付（返回表单） |
| 金额单位 | 最小货币单位（分/美分） | 分 | 元（字符串） |
| 回调 | 签名 JSON | AES-GCM 加密 + RSA 验签 | 表单编码 + RSA2 验签 |
| 成功标志 | `checkout.session.completed` | `trade_state = SUCCESS` | `TRADE_SUCCESS` / `TRADE_FINISHED` |

**凭据加密存储。** 私钥与密钥以 AES-GCM 加密后写入数据库（`ChannelSecretCodec`），主密钥来自 `ECOM_PAY_SECRET`。管理端只返回「是否已配置」，不回显明文；表单留空表示保持原值。这样一次数据库导出不会泄出可用的 Stripe 密钥。

**回调幂等。** 支付流水表的 `settle` 是带 `status = 0` 条件的更新，配合 `(channel_code, trade_no)` 唯一索引，重复回调只会成功一次。金额用 `compareTo` 比对——`10.0` 与 `10.00` 是同一笔钱，但 `equals` 会判它们不等。

**不在浏览器回跳时标记成功。** 回跳 URL 由用户控制，`?result=success` 可以伪造；只有验签过的回调才结算订单。

### 6. 小程序登录

`POST /api/app/auth/login/mp` 接收 `wx.login()` 的 code。演示环境未配置 AppID 时，服务端从 code 派生一个稳定的伪 openid，因此整条流程可测；配置 `WECHAT_APP_ID` / `WECHAT_APP_SECRET` 后可接入真实 `jscode2session`。

C 端 H5 也能被小程序直接加载（`nginx` 已透传 `X-Client-Platform`，前端通过 `?token=` 吸收宿主 token），订单因此能区分来源（`web` / `mp`）。

### 7. 文件存储

默认写本地磁盘（`ecom-uploads` 卷，nginx 以 `/uploads` 提供）。改为对象存储只需：

```env
STORAGE_TYPE=s3
S3_ENDPOINT=https://s3.amazonaws.com
S3_BUCKET=ecom-media
S3_ACCESS_KEY=...
S3_SECRET_KEY=...
```

AWS S3 / MinIO / 阿里云 OSS 的 S3 网关均可，业务代码无需改动。

### 8. 双主题图片

PC 商城有浅色 / 深色两套主题，照片也跟着换：同一件商品在深色下是暗调棚拍，在浅色下是白底高调棚拍。

数据库只存一份 `cover`，浅色版本按目录约定推导，不用加字段：

```
/images/products/helmet.jpg  →  /images/products/light/helmet.jpg
/images/hero/hero-1.jpg      →  /images/hero/light/hero-1.jpg
```

`src/components/Photo.tsx` 负责这套映射：主题为 light 时改写路径，图片 404 时自动回退到深色原图，所以某个商品缺浅色图也不会白屏。新增图片时把浅色版放进对应的 `light/` 目录即可，无需改代码。

### 9. 多语言

PC 商城支持简体中文 / English / 日本語，默认跟随浏览器语言，可通过头部地球图标切换并记忆到 `localStorage`。

分两层，各自放在适合的地方：

**界面文案**在前端。`src/i18n/messages.ts` 是唯一字典，中文是源语言的形状，另外两个语言声明为 `Record<MessageKey, string>` —— 少一条就编译报错，不会出现半句中文半句英文的页面。切换语言只重渲染，无请求。

**商品内容**在后端。分类名、商品名/副标题/详情、品牌描述、券名、轮播标题、公告都来自数据库，用一张覆盖表 `shop_i18n_text`（`entity, entity_id, field, lang`）加一张小词表 `shop_i18n_term`（规格名 / 规格值 / 单位，按中文字符串自身做键）。中文永远留在原表里，所以重置演示数据不依赖翻译表，缺译也会自然回退到中文。

语言协商顺序：`?lang=` 查询参数 → `X-Lang` 请求头 → `Accept-Language`（按 q 值取最优）。前端非中文时会给 GET 请求带上 `?lang=`，因为商品接口是可缓存的，只发请求头的话浏览器可能复用之前缓存的中文响应。

订单行不冻结译文：`shop_order_item` 里的 `product_name` 始终是中文快照，展示时按 `product_id` 翻译，这样用户切换语言后历史订单也会跟着变。

翻译表由 `scripts/gen_i18n_sql.py` 生成，改文案请改脚本再重新生成，不要手改 SQL。

---

## 数据库与迁移

所有表结构由 Flyway 托管，位于 `ecom-boot/src/main/resources/db/migration/`：

| 脚本 | 内容 |
| --- | --- |
| `V1__init_schema.sql` | 基础表 + 索引 + 报表视图 `v_order_daily` |
| `V2__seed_data.sql` | 分类/品牌/商品/SKU/会员/优惠券/近 30 天订单 |
| `V3__i18n_text.sql` | 英/日译文覆盖表与词表（由 `scripts/gen_i18n_sql.py` 生成） |
| `V4__pay_channel.sql` | 支付渠道配置表，并扩展支付流水字段 |

主要表：

| 表 | 说明 |
| --- | --- |
| `sys_admin` | 后台运营账号（BCrypt） |
| `sys_operation_log` | `@OperationLog` 切面写入的审计日志 |
| `shop_member` / `shop_member_address` | 会员与其地址簿 |
| `shop_category` / `shop_brand` | 两级分类树、品牌 |
| `shop_product` / `shop_product_sku` | SPU 与可售 SKU |
| `shop_cart` | 购物车（member+sku 唯一） |
| `shop_order` / `shop_order_item` | 订单头与下单快照 |
| `shop_pay_record` | 支付流水（含退款负数行，含渠道与渠道交易号） |
| `shop_pay_channel` | 支付渠道配置（凭据加密存储） |
| `shop_coupon` / `shop_member_coupon` | 券模板与会员持券 |
| `shop_banner` / `shop_notice` | 首页运营位 |
| `shop_i18n_text` / `shop_i18n_term` | 多语言译文覆盖（中文不入表） |

金额一律 `numeric(12,2)`，软删除统一用 `deleted` 字段，主键 `bigserial`。

**重置演示数据**

```bash
docker compose down -v && docker compose up -d
```

---

## 功能清单

**运营后台**（`ecom-admin-web`）

- 数据看板：KPI、14 天成交趋势（手写 SVG，无图表库）、品类销量、会员等级分布、热销榜、高价值会员
- 商品管理：SPU/SKU 矩阵生成、规格组合、库存、上下架、图片上传
- 分类 / 品牌 / 轮播 / 公告
- 订单管理：状态筛选、搜索、详情抽屉、发货、商家备注、退款
- 会员管理：来源与状态筛选、详情、启用/禁用
- **支付渠道**：Stripe / 微信 / 支付宝 / 模拟支付的启停、凭据配置、配置自检，凭据加密存储
- 优惠券：满减/折扣券、领取进度
- 操作日志、存储配置概览
- 布局：52px 顶栏、208px 可折叠侧栏，菜单与内容同一浅色表面

**C 端 · PC Web**（`ecom-app-web`）

- 桌面优先的编辑式排版：衬线标题配无衬线正文，暖白纸感底色，单一强调色
- 首页：全幅 hero 轮播、服务承诺条、品类图墙、编辑推荐、主推特稿、活动横幅、热销榜
- 商品列表：左侧固定筛选栏（品类树 + 价格区间）、排序胶囊、分页加载
- 商品详情：竖排缩略图 + 大图、规格矩阵、库存约束、加购/立即购买
- 购物车与结算：双栏布局，右栏 sticky 订单摘要
- 支付：按运营开启的渠道动态渲染；支持跳转、二维码轮询、页内完成三种形态
- 账户中心：概览、订单（状态筛选）、订单详情、优惠券、地址簿、资料
- 响应式：≤1024px 收为单栏，≤900px 侧栏折叠为「筛选」按钮、导航收进汉堡菜单，≤640px 商品网格两列
- **双主题**：浅色（默认）/ 深色，配图与遮罩同步切换
- **多语言**：简体中文 / English / 日本語，切换即时生效并记忆偏好

**C 端 · H5 / 小程序**（`ecom-app-mini`）

- 首页：轮播、快捷入口、公告跑马灯、分类宫格、推荐/新品/热销
- 分类页（左侧栏 + 右侧商品）、搜索与排序、分页加载
- 商品详情：图集、规格选择、SKU 匹配、库存约束、加购/立即购买
- 购物车：勾选、改数量、删除、全选、合计
- 结算：地址选择、优惠券选择（含关闭自动优惠）、支付方式、备注
- 订单：列表（按状态）、详情、支付、取消、确认收货
- 个人中心：订单角标、优惠券、地址簿、资料编辑
- 登录：密码 / 短信验证码（演示环境回显验证码）/ 微信一键登录

---

## 测试

```bash
# 端到端接口冒烟测试（覆盖完整购买链路 + 权限隔离）
python3 scripts/smoke_test.py http://localhost:8080
```

会打印一张 24 项的检查表，覆盖浏览、加购、结算、支付、发货、收货、商品增删改、审计日志，以及三类越权访问。

---

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 模块划分、依赖方向、请求链路、支付与多语言子系统、数据模型、已知约束 |
| [FEATURES.md](docs/FEATURES.md) | 三端功能逐项说明、业务规则、演示账号与数据规模 |
| [API.md](docs/API.md) | 接口约定、鉴权方式、响应信封 |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | 镜像构成、构建耗时、生产环境注意事项 |

---

## 生产部署注意事项

1. **务必覆盖密钥**：`JWT_SECRET`（≥32 字符）、`DB_PASSWORD`、`REDIS_PASSWORD`。`deploy.sh` 首次运行会自动生成随机 `JWT_SECRET`。
2. **支付主密钥**：`ECOM_PAY_SECRET`（≥16 字符）用于加密渠道凭据。**未设置时凭据会明文落库**，启动日志会告警。轮换它需要重新录入全部渠道密钥。
3. **支付回调地址**：`ECOM_PUBLIC_URL` 必须是公网可达的绝对地址（Stripe 与支付宝不接受相对路径）。回调路径为 `/api/app/pay/notify/{channel}`。
4. **关闭演示渠道**：上线前在后台停用「模拟支付」，否则客户可以零成本下单。
5. **关闭演示回显**：`AppAuthController#sendSms` 会把验证码放在 `demoCode` 字段返回，接入真实短信服务后请删除。
6. **修改演示账号**：上线前更换 `admin` 密码，或删除 `V2` 中的种子账号。
7. **HTTPS**：在生产入口（负载均衡 / 反向代理）终止 TLS，`docker-compose.yml` 暴露的是明文 HTTP。
8. **上传目录**：多实例部署时把 `ecom-uploads` 卷换成共享存储，或切换到 S3。
9. **备份**：定期 `pg_dump`；Redis 仅承载可重建的缓存与限流数据，无需强一致备份。
10. **定时任务**：订单超时关闭与自动收货依赖 `@Scheduled`，多实例下幂等但会产生重复日志，如介意可改为分布式锁独占。
11. **登出不会使 token 失效**：JWT 无状态，`/logout` 仅清客户端状态；黑名单 key 前缀已预留但未实现写入。token 默认有效期 7 天，期间被登出的凭证仍可用。若需要即时失效，在登出时写 `jti` 到 Redis 并在拦截器校验。
12. **会员等级不会自动升级**：新会员固定 1 级，`level` 目前只是展示字段。

---

## 技术栈

| 层 | 选型 |
| --- | --- |
| 运行时 | JDK 17 · Spring Boot 2.7.13 |
| 持久化 | MyBatis-Plus 3.5 · PostgreSQL 16 · Flyway 8 |
| 缓存 | Redis 7（Lettuce 连接池） |
| 鉴权 | JJWT 0.11（HS256 + audience 隔离）· BCrypt |
| 文档 | springdoc-openapi 1.7 |
| 支付 | stripe-java 29 · wechatpay-java 0.2（APIv3）· alipay-sdk-java 4.40 |
| 前端 | React 18 · TypeScript · Vite 5 |
| 运营端 UI | Ant Design 5 |
| C 端 UI | 手写 CSS（无 UI 框架，控制包体积） |
| 部署 | Docker Compose · Nginx 1.27 |

---

## License

[MIT](LICENSE) © Ecom contributors
