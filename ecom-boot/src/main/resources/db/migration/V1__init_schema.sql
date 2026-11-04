-- =====================================================================
--  ecom platform · initial schema (PostgreSQL 12+)
--
--  Conventions
--   * bigserial primary keys
--   * money is numeric(12,2), never float
--   * every business table carries created_at / updated_at
--   * soft delete uses `deleted` (0 = alive, 1 = removed)
--   * reserved SQL words are avoided in identifiers (description, not desc)
-- =====================================================================

create sequence if not exists seq_order_no start 100000;

-- ---------------------------------------------------------------------
--  Back office operators
-- ---------------------------------------------------------------------
create table sys_admin
(
    id          bigserial primary key,
    username    varchar(64)  not null,
    password    varchar(100) not null,
    nickname    varchar(64),
    avatar      varchar(500),
    phone       varchar(32),
    email       varchar(128),
    role        varchar(32)  not null default 'ROLE_ADMIN',
    enabled     smallint     not null default 1,
    last_login_at timestamp,
    created_at  timestamp    not null default now(),
    updated_at  timestamp    not null default now(),
    deleted     smallint     not null default 0
);
create unique index uk_sys_admin_username on sys_admin (username) where deleted = 0;

-- ---------------------------------------------------------------------
--  Operation audit log (filled by the logging module aspect)
-- ---------------------------------------------------------------------
create table sys_operation_log
(
    id          bigserial primary key,
    operator_id bigint,
    operator    varchar(64),
    module      varchar(64),
    action      varchar(128),
    method      varchar(255),
    uri         varchar(255),
    params      text,
    ip          varchar(64),
    duration_ms bigint,
    success     smallint not null default 1,
    error_msg   varchar(1000),
    created_at  timestamp not null default now()
);
create index idx_sys_oplog_created on sys_operation_log (created_at desc);
create index idx_sys_oplog_operator on sys_operation_log (operator_id);

-- ---------------------------------------------------------------------
--  Store customers (web + mini program)
-- ---------------------------------------------------------------------
create table shop_member
(
    id              bigserial primary key,
    mobile          varchar(32),
    password        varchar(100),
    nickname        varchar(64),
    avatar          varchar(500),
    gender          smallint     not null default 0, -- 0 unknown / 1 male / 2 female
    birthday        date,
    wx_openid       varchar(64),
    wx_unionid      varchar(64),
    platform        varchar(16)  not null default 'web', -- web | mp
    level           integer      not null default 1,
    points          integer      not null default 0,
    balance         numeric(12, 2) not null default 0,
    total_spent     numeric(12, 2) not null default 0,
    order_count     integer      not null default 0,
    status          smallint     not null default 1, -- 1 normal / 0 disabled
    last_login_at   timestamp,
    created_at      timestamp    not null default now(),
    updated_at      timestamp    not null default now(),
    deleted         smallint     not null default 0
);
create unique index uk_member_mobile on shop_member (mobile) where mobile is not null and deleted = 0;
create unique index uk_member_openid on shop_member (wx_openid) where wx_openid is not null and deleted = 0;

create table shop_member_address
(
    id           bigserial primary key,
    member_id    bigint       not null,
    receiver     varchar(64)  not null,
    mobile       varchar(32)  not null,
    province     varchar(64),
    city         varchar(64),
    district     varchar(64),
    detail       varchar(255) not null,
    postal_code  varchar(16),
    is_default   smallint     not null default 0,
    created_at   timestamp    not null default now(),
    updated_at   timestamp    not null default now(),
    deleted      smallint     not null default 0
);
create index idx_address_member on shop_member_address (member_id);

-- ---------------------------------------------------------------------
--  Catalog
-- ---------------------------------------------------------------------
create table shop_category
(
    id          bigserial primary key,
    parent_id   bigint       not null default 0,
    name        varchar(64)  not null,
    icon        varchar(500),
    banner      varchar(500),
    level       integer      not null default 1,
    sort        integer      not null default 0,
    visible     smallint     not null default 1,
    created_at  timestamp    not null default now(),
    updated_at  timestamp    not null default now(),
    deleted     smallint     not null default 0
);
create index idx_category_parent on shop_category (parent_id);

create table shop_brand
(
    id          bigserial primary key,
    name        varchar(64) not null,
    logo        varchar(500),
    description varchar(1000),
    sort        integer     not null default 0,
    visible     smallint    not null default 1,
    created_at  timestamp   not null default now(),
    updated_at  timestamp   not null default now(),
    deleted     smallint    not null default 0
);

create table shop_product
(
    id             bigserial primary key,
    category_id    bigint         not null,
    brand_id       bigint,
    name           varchar(160)   not null,
    subtitle       varchar(255),
    cover          varchar(500),
    images         text,          -- jsonb array of urls, kept as text for portability
    detail         text,
    price          numeric(12, 2) not null default 0, -- lowest sku price, denormalised
    original_price numeric(12, 2),
    stock          integer        not null default 0, -- sum of sku stock
    sales          integer        not null default 0,
    views          integer        not null default 0,
    unit           varchar(16),
    specs          text,          -- jsonb array of {name, values[]}
    keywords       varchar(255),
    is_new         smallint       not null default 0,
    is_hot         smallint       not null default 0,
    is_recommend   smallint       not null default 0,
    status         smallint       not null default 1, -- 1 on sale / 0 off shelf
    sort           integer        not null default 0,
    created_at     timestamp      not null default now(),
    updated_at     timestamp      not null default now(),
    deleted        smallint       not null default 0
);
create index idx_product_category on shop_product (category_id);
create index idx_product_status on shop_product (status, deleted);
create index idx_product_created on shop_product (created_at desc);

create table shop_product_sku
(
    id          bigserial primary key,
    product_id  bigint         not null,
    sku_code    varchar(64),
    spec_values varchar(255),  -- "白色,128G"
    image       varchar(500),
    price       numeric(12, 2) not null default 0,
    stock       integer        not null default 0,
    sales       integer        not null default 0,
    created_at  timestamp      not null default now(),
    updated_at  timestamp      not null default now(),
    deleted     smallint       not null default 0
);
create index idx_sku_product on shop_product_sku (product_id);

-- ---------------------------------------------------------------------
--  Marketing
-- ---------------------------------------------------------------------
create table shop_banner
(
    id          bigserial primary key,
    title       varchar(128),
    image       varchar(500) not null,
    link_type   varchar(16)  not null default 'none', -- none | product | category | url
    link_value  varchar(255),
    position    varchar(16)  not null default 'home', -- home | category | mine
    sort        integer      not null default 0,
    enabled     smallint     not null default 1,
    created_at  timestamp    not null default now(),
    updated_at  timestamp    not null default now(),
    deleted     smallint     not null default 0
);

create table shop_notice
(
    id          bigserial primary key,
    title       varchar(128) not null,
    content     text,
    enabled     smallint     not null default 1,
    sort        integer      not null default 0,
    created_at  timestamp    not null default now(),
    updated_at  timestamp    not null default now(),
    deleted     smallint     not null default 0
);

create table shop_coupon
(
    id             bigserial primary key,
    name           varchar(64)    not null,
    type           varchar(16)    not null default 'amount', -- amount | discount
    amount         numeric(12, 2) not null default 0,        -- for type=amount
    discount       numeric(4, 2)  not null default 10,       -- for type=discount, e.g. 8.5 = 85%
    min_amount     numeric(12, 2) not null default 0,
    total          integer        not null default 0,
    taken          integer        not null default 0,
    valid_days     integer        not null default 7,        -- relative validity after receive
    start_at       timestamp,
    end_at         timestamp,
    enabled        smallint       not null default 1,
    created_at     timestamp      not null default now(),
    updated_at     timestamp      not null default now(),
    deleted        smallint       not null default 0
);

create table shop_member_coupon
(
    id          bigserial primary key,
    coupon_id   bigint      not null,
    member_id   bigint      not null,
    status      smallint    not null default 0, -- 0 unused / 1 used / 2 expired
    order_no    varchar(32),
    start_at    timestamp   not null,
    end_at      timestamp   not null,
    used_at     timestamp,
    created_at  timestamp   not null default now(),
    updated_at  timestamp   not null default now()
);
create index idx_member_coupon_member on shop_member_coupon (member_id, status);

-- ---------------------------------------------------------------------
--  Cart
-- ---------------------------------------------------------------------
create table shop_cart
(
    id          bigserial primary key,
    member_id   bigint   not null,
    product_id  bigint   not null,
    sku_id      bigint   not null,
    quantity    integer  not null default 1,
    checked     smallint not null default 1,
    created_at  timestamp not null default now(),
    updated_at  timestamp not null default now()
);
create unique index uk_cart_member_sku on shop_cart (member_id, sku_id);

-- ---------------------------------------------------------------------
--  Orders
-- ---------------------------------------------------------------------
create table shop_order
(
    id                bigserial primary key,
    order_no          varchar(32)    not null,
    member_id         bigint         not null,
    member_nickname   varchar(64),
    status            smallint       not null default 0,
        -- 0 pending payment / 1 pending shipment / 2 shipped / 3 completed
        -- 4 cancelled / 5 refunding / 6 refunded
    total_amount      numeric(12, 2) not null default 0, -- goods subtotal
    freight_amount    numeric(12, 2) not null default 0,
    discount_amount   numeric(12, 2) not null default 0,
    pay_amount        numeric(12, 2) not null default 0,
    pay_type          varchar(16),                       -- wechat | alipay | balance | mock
    paid_at           timestamp,
    receiver          varchar(64)    not null,
    receiver_mobile   varchar(32)    not null,
    receiver_province varchar(64),
    receiver_city     varchar(64),
    receiver_district varchar(64),
    receiver_detail   varchar(255)   not null,
    remark            varchar(500),
    coupon_id         bigint,
    platform          varchar(16)    not null default 'web',
    express_company   varchar(64),
    express_no        varchar(64),
    shipped_at        timestamp,
    finished_at       timestamp,
    closed_at         timestamp,
    admin_remark      varchar(500),
    created_at        timestamp      not null default now(),
    updated_at        timestamp      not null default now(),
    deleted           smallint       not null default 0
);
create unique index uk_order_no on shop_order (order_no);
create index idx_order_member on shop_order (member_id, status);
create index idx_order_status on shop_order (status, created_at desc);
create index idx_order_created on shop_order (created_at desc);

create table shop_order_item
(
    id            bigserial primary key,
    order_no      varchar(32)    not null,
    product_id    bigint         not null,
    sku_id        bigint         not null,
    product_name  varchar(160)   not null,
    product_cover varchar(500),
    spec_values   varchar(255),
    price         numeric(12, 2) not null,
    quantity      integer        not null,
    total_amount  numeric(12, 2) not null,
    created_at    timestamp      not null default now()
);
create index idx_order_item_order on shop_order_item (order_no);

create table shop_pay_record
(
    id          bigserial primary key,
    order_no    varchar(32)    not null,
    member_id   bigint         not null,
    pay_type    varchar(16)    not null,
    amount      numeric(12, 2) not null,
    trade_no    varchar(64),
    status      smallint       not null default 0, -- 0 pending / 1 success / 2 failed
    paid_at     timestamp,
    created_at  timestamp      not null default now()
);
create index idx_pay_record_order on shop_pay_record (order_no);

-- ---------------------------------------------------------------------
--  Denormalised sales counter refresh helper (kept as a view for reporting)
-- ---------------------------------------------------------------------
create or replace view v_order_daily as
select to_char(created_at, 'YYYY-MM-DD') as day,
       count(*)                            as order_count,
       coalesce(sum(pay_amount), 0)        as gmv
from shop_order
where deleted = 0
  and status in (1, 2, 3)
group by 1;
