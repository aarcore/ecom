-- =====================================================================
--  ecom platform · demo seed data (mountain bike store)
--
--  Login credentials
--    admin console : admin / admin123
--    storefront    : 13800138000 / 123456
--
--  Catalogue shape: 6 root categories x 2 sub-categories, 24 products with
--  2 per sub-category so every branch is populated in the demo.
-- =====================================================================

-- ---------------------------------------------------------------------
--  Operators
-- ---------------------------------------------------------------------
insert into sys_admin (username, password, nickname, avatar, phone, email, role)
values ('admin', '$2a$10$GIsZeu4PgOk1aswmMXdRX.dWdWhu76kzkS7JL4m5j73Im51AmET5q',
        '超级管理员', '/images/avatars/admin.svg', '13900000000', 'admin@ecom.dev', 'ROLE_ADMIN');

insert into sys_admin (username, password, nickname, avatar, phone, email, role)
values ('operator', '$2a$10$GIsZeu4PgOk1aswmMXdRX.dWdWhu76kzkS7JL4m5j73Im51AmET5q',
        '运营专员', '/images/avatars/operator.svg', '13900000001', 'ops@ecom.dev', 'ROLE_OPERATOR');

-- ---------------------------------------------------------------------
--  Categories (two levels)
-- ---------------------------------------------------------------------
insert into shop_category (id, parent_id, name, icon, banner, level, sort, visible) values
  (1, 0, '山地整车', '🚵', '/images/products/bike-hardtail.jpg', 1, 100, 1),
  (2, 0, '头盔护具', '🪖', '/images/products/helmet.jpg', 1, 90, 1),
  (3, 0, '骑行服饰', '👕', '/images/products/jersey.jpg', 1, 80, 1),
  (4, 0, '传动制动', '⚙️', '/images/products/drivetrain.jpg', 1, 70, 1),
  (5, 0, '轮组轮胎', '🛞', '/images/products/wheel.jpg', 1, 60, 1),
  (6, 0, '避震车架', '🔧', '/images/products/fork.jpg', 1, 50, 1);

insert into shop_category (id, parent_id, name, icon, level, sort, visible) values
  (11, 1, '硬尾山地车', '🚲', 2, 100, 1),
  (12, 1, '软尾山地车', '🚵', 2, 90, 1),
  (21, 2, '头盔', '🪖', 2, 100, 1),
  (22, 2, '护膝护具', '🛡️', 2, 90, 1),
  (31, 3, '骑行服', '👕', 2, 100, 1),
  (32, 3, '骑行鞋与手套', '👟', 2, 90, 1),
  (41, 4, '变速套件', '⛓️', 2, 100, 1),
  (42, 4, '刹车与碟片', '🛑', 2, 90, 1),
  (51, 5, '轮组', '🛞', 2, 100, 1),
  (52, 5, '外胎与内胎', '🕳️', 2, 90, 1),
  (61, 6, '前叉避震', '🔩', 2, 100, 1),
  (62, 6, '车架与后避震', '🔧', 2, 90, 1);

select setval('shop_category_id_seq', 100);

-- ---------------------------------------------------------------------
--  Brands
-- ---------------------------------------------------------------------
insert into shop_brand (id, name, logo, description, sort) values
  (1, 'Summit', '/images/brands/summit.svg', '竞赛级山地车整车品牌，主打轻量化车架', 100),
  (2, 'Ridge Line', '/images/brands/ridgeline.svg', '硬尾与 XC 车型，性价比取向', 90),
  (3, 'Torque Lab', '/images/brands/torque.svg', '避震与传动系统研发，专注极限路况', 80),
  (4, 'Crest', '/images/brands/crest.svg', '轮组、头盔与骑行防护装备', 70),
  (5, 'Trailhead', '/images/brands/trailhead.svg', '骑行服饰、鞋履与随身包具', 60);
select setval('shop_brand_id_seq', 100);

-- ---------------------------------------------------------------------
--  Products
-- ---------------------------------------------------------------------
insert into shop_product
  (id, category_id, brand_id, name, subtitle, cover, images, detail, price, original_price,
   stock, sales, views, unit, specs, keywords, is_new, is_hot, is_recommend, status, sort, created_at)
values
  (1, 11, 1, 'Summit Trail 29 硬尾山地车',
   '29 寸轮径 · 铝合金车架 · 12 速变速 · 100mm 前叉',
   '/images/products/bike-hardtail.jpg',
   '["/images/products/bike-hardtail.jpg"]',
   'Trail 29 是 Summit 面向日常越野与轻度林道的主力硬尾。6061 铝合金车架采用液压成型工艺，五通刚性充足，后三角留有 2.4 寸胎宽余量。配 100mm 气压前叉与 12 速单盘传动，爬坡齿比覆盖足够宽，新手也能从容应对连续陡坡。',
   3299.00, 3899.00, 86, 412, 8640, '台',
   '[{"name":"车架尺码","values":["S","M","L","XL"]},{"name":"颜色","values":["哑光黑","林道绿"]}]',
   '山地车,硬尾,29寸,12速', 1, 1, 1, 1, 100, now() - interval '2 day'),

  (2, 11, 2, 'Ridge Line XC 200 硬尾山地车',
   'XC 几何 · 液压碟刹 · 气室前叉 · 适合越野竞速入门',
   '/images/products/bike-hardtail.jpg',
   '["/images/products/bike-hardtail.jpg"]',
   'XC 200 采用偏竞速的 69° 头管角度与较短后下叉，转向灵活。整车出厂即配液压碟刹与 2.25 寸外胎，适合越野赛道与城市林道混合路况。',
   4599.00, 5299.00, 54, 268, 5210, '台',
   '[{"name":"车架尺码","values":["S","M","L"]},{"name":"颜色","values":["珍珠白","赛道蓝"]}]',
   '山地车,硬尾,XC,液压碟刹', 1, 0, 1, 1, 95, now() - interval '5 day'),

  (3, 12, 1, 'Summit Enduro 170 软尾山地车',
   '170mm 行程 · 四连杆避震 · 碳纤维前三角',
   '/images/products/bike-full.jpg',
   '["/images/products/bike-full.jpg"]',
   'Enduro 170 为长行程林道与耐力赛设计。四连杆结构在踩踏与吸震之间取得平衡，170mm 前后行程搭配渐进式弹簧曲线，落地支撑充足。碳纤维前三角让整车重量控制在 14.2kg。',
   12800.00, 14900.00, 28, 96, 4120, '台',
   '[{"name":"车架尺码","values":["S","M","L","XL"]},{"name":"颜色","values":["碳黑","熔岩橙"]}]',
   '山地车,软尾,enduro,碳纤维', 0, 1, 1, 1, 90, now() - interval '9 day'),

  (4, 12, 3, 'Torque Lab AM 150 软尾山地车',
   '150mm 行程 · 铝合金车架 · 全能林道取向',
   '/images/products/bike-full.jpg',
   '["/images/products/bike-full.jpg"]',
   'AM 150 是「一辆车骑所有路」的答案。65° 头管角度兼顾下坡稳定性与爬坡效率，150mm 行程覆盖绝大多数林道场景。整车预留水壶架与工具挂点，适合长距离穿越。',
   9800.00, 11900.00, 34, 142, 3860, '台',
   '[{"name":"车架尺码","values":["M","L","XL"]},{"name":"颜色","values":["哑光石墨","军绿"]}]',
   '山地车,软尾,林道,铝合金', 0, 0, 1, 1, 85, now() - interval '12 day'),

  (5, 21, 4, 'Crest Airflow MIPS 山地头盔',
   'MIPS 旋转防护 · 18 个通风孔 · 可调帽檐',
   '/images/products/helmet.jpg',
   '["/images/products/helmet.jpg"]',
   'Airflow 采用 MIPS 低摩擦层，在斜向冲击时降低脑部旋转加速度。18 个风道配合内部导流槽，低速爬坡也能保持通风。旋钮式后枕调节支持单手操作。',
   699.00, 899.00, 320, 1240, 15200, '顶',
   '[{"name":"尺码","values":["S/M","L/XL"]},{"name":"颜色","values":["哑光黑","云白","荧光绿"]}]',
   '头盔,MIPS,山地,防护', 1, 1, 1, 1, 99, now() - interval '1 day'),

  (6, 21, 4, 'Crest Half Shell 半盔',
   '轻量 280g · 12 风道 · 适合 XC 与城市林道',
   '/images/products/helmet.jpg',
   '["/images/products/helmet.jpg"]',
   'Half Shell 定位轻量越野，280g 重量长时间佩戴无负担。后脑覆盖范围比常规公路头盔多出 3cm，兼顾保护与通风。',
   399.00, 499.00, 480, 860, 9400, '顶',
   '[{"name":"尺码","values":["S/M","L/XL"]},{"name":"颜色","values":["黑","白"]}]',
   '头盔,半盔,轻量', 0, 0, 0, 1, 70, now() - interval '16 day'),

  (7, 22, 5, 'Trailhead 护膝护肘套装',
   'D3O 缓震内衬 · 防滑硅胶条 · 透气网布',
   '/images/products/pads.jpg',
   '["/images/products/pads.jpg"]',
   '套装包含护膝与护肘各一对。D3O 内衬在常态下柔软贴合，受冲击时瞬间硬化分散能量。上下端防滑硅胶条保证长时间骑行不位移。',
   559.00, 699.00, 210, 340, 4200, '套',
   '[{"name":"尺码","values":["S","M","L","XL"]}]',
   '护具,护膝,护肘,D3O', 0, 1, 1, 1, 65, now() - interval '4 day'),

  (8, 22, 5, 'Trailhead 轻量护膝',
   '单只 120g · 透气穿孔 · 适合日常林道',
   '/images/products/pads.jpg',
   '["/images/products/pads.jpg"]',
   '针对踩踏频繁的林道路况，轻量护膝采用穿孔 EVA 内衬，兼顾防护与散热。弹力织带固定，不压迫膝窝血管。',
   259.00, 329.00, 260, 410, 3800, '只',
   '[{"name":"尺码","values":["S","M","L","XL"]}]',
   '护膝,轻量,林道', 0, 0, 0, 1, 60, now() - interval '20 day'),

  (9, 31, 5, 'Trailhead 速干骑行服',
   '四向弹力 · 后摆加长 · 背侧拉链口袋',
   '/images/products/jersey.jpg',
   '["/images/products/jersey.jpg"]',
   '前襟采用高透气网眼面料，背部速干处理加速排汗。下摆后侧加长 4cm，骑行姿势下依然覆盖腰部。背侧三格口袋可放手机与能量胶。',
   459.00, 599.00, 340, 620, 7100, '件',
   '[{"name":"尺码","values":["S","M","L","XL"]},{"name":"颜色","values":["苔绿","石墨灰","砖红"]}]',
   '骑行服,速干,弹力', 1, 0, 1, 1, 55, now() - interval '3 day'),

  (10, 31, 5, 'Trailhead 防风骑行马甲',
   '轻量防风 · 可收纳进后袋 · 反光标识',
   '/images/products/jersey.jpg',
   '["/images/products/jersey.jpg"]',
   '上山时塞进后袋，下山前穿上挡风。整件 96g，面料防风但不完全防水，适合温差变化大的山地环境。',
   329.00, 429.00, 280, 330, 3900, '件',
   '[{"name":"尺码","values":["S","M","L","XL"]},{"name":"颜色","values":["黑","荧光黄"]}]',
   '马甲,防风,轻量', 0, 0, 0, 1, 50, now() - interval '23 day'),

  (11, 32, 5, 'Trailhead 锁踏骑行鞋',
   '碳复合鞋底 · 可调鞋钉 · 防水鞋面',
   '/images/products/shoe.jpg',
   '["/images/products/shoe.jpg"]',
   '鞋底采用碳复合中底，踩踏刚性足够但保留离车推行时的弯折余量。前掌两颗可调鞋钉用于松软路面，鞋面经防泼水处理。',
   899.00, 1099.00, 190, 380, 5600, '双',
   '[{"name":"尺码","values":["40","41","42","43","44"]},{"name":"颜色","values":["哑光黑","沙棕"]}]',
   '骑行鞋,锁踏,山地', 0, 1, 1, 1, 45, now() - interval '6 day'),

  (12, 32, 5, 'Trailhead 减震骑行手套',
   '掌垫缓冲 · 半指设计 · 指尖触屏',
   '/images/products/pads.jpg',
   '["/images/products/pads.jpg"]',
   '掌心硅胶垫分散长时间握把的压迫，半指设计保留操作变速的灵活度。拇指与食指支持触屏，无需摘手套。',
   159.00, 199.00, 620, 980, 11200, '双',
   '[{"name":"尺码","values":["S","M","L","XL"]},{"name":"颜色","values":["黑","灰"]}]',
   '手套,减震,半指', 0, 0, 0, 1, 40, now() - interval '14 day'),

  (13, 41, 3, 'Torque Lab 12 速变速套件',
   '12 速 · 10-51T 飞轮 · 单盘系统 · 含指拨后拨',
   '/images/products/drivetrain.jpg',
   '["/images/products/drivetrain.jpg"]',
   '整套包含指拨、后拨、飞轮与链条。510% 齿比范围覆盖陡坡爬升与平路冲刺，后拨配备离合阻尼，颠簸路面不掉链。',
   3299.00, 3799.00, 120, 240, 3400, '套',
   '[{"name":"版本","values":["含曲柄","不含曲柄"]}]',
   '变速套件,12速,单盘', 1, 0, 1, 1, 35, now() - interval '7 day'),

  (14, 41, 2, 'Ridge Line 11 速指拨后拨',
   '11 速 · 长腿后拨 · 兼容 46T 飞轮',
   '/images/products/drivetrain.jpg',
   '["/images/products/drivetrain.jpg"]',
   '入门升级的常见选择。长腿后拨兼容最大 46T 飞轮，双释放指拨支持连续降档，爬坡时换挡更果断。',
   899.00, 1099.00, 210, 310, 3200, '套',
   '[{"name":"版本","values":["右侧单拨","左右套装"]}]',
   '指拨,后拨,11速', 0, 0, 0, 1, 30, now() - interval '18 day'),

  (15, 42, 3, 'Torque Lab 四活塞油压碟刹',
   '四活塞卡钳 · 陶瓷活塞 · 含碟片与来令片',
   '/images/products/brake.jpg',
   '["/images/products/brake.jpg"]',
   '四活塞结构在长下坡连续制动时抗热衰减明显优于双活塞。陶瓷活塞隔热性好，减少刹车油沸腾风险。出厂已注油并完成排气。',
   1499.00, 1799.00, 160, 290, 4100, '套',
   '[{"name":"安装标准","values":["Post Mount","IS 转接"]}]',
   '碟刹,四活塞,油压', 0, 1, 1, 1, 28, now() - interval '8 day'),

  (16, 42, 3, 'Torque Lab 203mm 浮动碟片',
   '203mm · 浮动结构 · 六钉安装',
   '/images/products/rotor.jpg',
   '["/images/products/rotor.jpg"]',
   '浮动碟片允许内外圈热膨胀差异，高温下不易变形。203mm 直径提供更大的制动力矩，适合长下坡与体重较大的骑手。',
   329.00, 399.00, 340, 520, 6300, '片',
   '[{"name":"直径","values":["180mm","203mm"]}]',
   '碟片,浮动,203mm', 0, 0, 0, 1, 25, now() - interval '11 day'),

  (17, 51, 4, 'Crest Carbon 29 碳纤维轮组',
   '29 寸 · 碳纤维圈 · 28mm 内宽 · 真空ready',
   '/images/products/wheel.jpg',
   '["/images/products/wheel.jpg"]',
   '28mm 内宽在 2.3-2.5 寸胎压下提供更稳定的胎型支撑。碳圈重量 1580g/对，五通发力传导直接。花鼓为 6 棘爪 84T 啮合。',
   5800.00, 6800.00, 46, 88, 2900, '对',
   '[{"name":"花鼓","values":["六钉/Boost","中锁/Boost"]}]',
   '轮组,碳纤维,29寸,真空', 1, 0, 1, 1, 22, now() - interval '5 day'),

  (18, 51, 4, 'Crest Alloy 29 铝合金轮组',
   '29 寸 · 铝合金圈 · 25mm 内宽 · 真空ready',
   '/images/products/wheel.jpg',
   '["/images/products/wheel.jpg"]',
   '性价比取向的铝合金轮组。25mm 内宽适配 2.2-2.4 寸外胎，出厂已贴真空胎垫并配气嘴，可直接改真空。',
   1899.00, 2299.00, 74, 156, 3600, '对',
   '[{"name":"花鼓","values":["六钉/Boost","中锁/Boost"]}]',
   '轮组,铝合金,29寸', 0, 0, 0, 1, 20, now() - interval '19 day'),

  (19, 52, 4, 'Crest 29x2.4 真空外胎',
   '29 x 2.4 · 双层胎体 · 中软复合橡胶',
   '/images/products/wheel.jpg',
   '["/images/products/wheel.jpg"]',
   '双层 120TPI 胎体兼顾防刺与路感。中软复合橡胶在湿滑树根与碎石路面抓地稳定，胎纹中置低滚阻、侧齿支撑过弯。',
   359.00, 429.00, 420, 760, 8800, '条',
   '[{"name":"规格","values":["29x2.2","29x2.4","29x2.6"]},{"name":"胎体","values":["120TPI","60TPI 防刺"]}]',
   '外胎,真空,29寸,山地', 0, 1, 1, 1, 18, now() - interval '10 day'),

  (20, 52, 4, 'Crest 真空胎补胎套装',
   '补胎条 · 插入工具 · 自补液 60ml',
   '/images/products/toolkit.jpg',
   '["/images/products/toolkit.jpg"]',
   '真空胎被扎后的现场急救方案。橡胶补胎条配合插入工具可在两分钟内完成免拆轮封堵，自补液补充装用于日常维护。',
   129.00, 169.00, 720, 1420, 16800, '套',
   '[]',
   '补胎,真空胎,工具', 0, 0, 0, 1, 15, now() - interval '13 day'),

  (21, 61, 3, 'Torque Lab 160mm 气压前叉',
   '160mm 行程 · 35mm 内管 · 气压弹簧 · 阻尼可调',
   '/images/products/fork.jpg',
   '["/images/products/fork.jpg"]',
   '35mm 内管在长行程下提供更好的抗扭刚性。气压弹簧支持气压与回弹调节，压缩阻尼三档可调，适应从碎石到连续落差的路况。',
   4200.00, 4900.00, 52, 78, 2600, '支',
   '[{"name":"行程","values":["140mm","160mm","170mm"]},{"name":"舵管","values":["锥形舵管","直管"]}]',
   '前叉,气压,避震,160mm', 1, 0, 1, 1, 12, now() - interval '2 day'),

  (22, 61, 3, 'Torque Lab 130mm 林道前叉',
   '130mm 行程 · 34mm 内管 · 轻量取向',
   '/images/products/fork.jpg',
   '["/images/products/fork.jpg"]',
   '面向短行程林道车，130mm 行程配合 34mm 内管，重量比 160mm 版本轻 320g。适合以踩踏效率为主的路线。',
   3400.00, 3999.00, 64, 92, 2400, '支',
   '[{"name":"行程","values":["120mm","130mm"]}]',
   '前叉,林道,130mm', 0, 0, 0, 1, 10, now() - interval '21 day'),

  (23, 62, 2, 'Ridge Line 铝合金硬尾车架',
   '6061 铝合金 · 29 寸 · Boost 开档 · 含碗组',
   '/images/products/frame.jpg',
   '["/images/products/frame.jpg"]',
   '单独出售的车架，适合自组或更换。液压成型管型在五通与头管处加厚，Boost 148 后开档兼容主流轮组。含碗组与座管夹。',
   2399.00, 2899.00, 38, 64, 1900, '个',
   '[{"name":"尺码","values":["S","M","L","XL"]},{"name":"涂装","values":["哑光黑","森林绿"]}]',
   '车架,铝合金,硬尾,29寸', 0, 0, 0, 1, 8, now() - interval '25 day'),

  (24, 62, 3, 'Torque Lab 后避震器 205x65',
   '205x65mm · 气压 · 三档压缩阻尼 · 带锁死',
   '/images/products/shock.jpg',
   '["/images/products/shock.jpg"]',
   '适配 150-170mm 行程软尾车架。三档压缩阻尼可在爬坡时切到最硬，下坡切回全开。带踩踏平台锁死拨杆。',
   2680.00, 3100.00, 0, 46, 1600, '支',
   '[{"name":"规格","values":["205x65","230x65"]}]',
   '后避震,气压,锁死', 0, 0, 0, 0, 5, now() - interval '35 day');

select setval('shop_product_id_seq', 200);

-- SKUs are generated deterministically from each product's spec matrix.
-- The first two spec dimensions are combined (max 9 SKUs); products without
-- a spec matrix get a single "标准" SKU.
with spec_src as (
    select p.id as product_id,
           p.price,
           p.original_price,
           p.stock,
           p.cover,
           case when jsonb_typeof(p.specs::jsonb) = 'array' and jsonb_array_length(p.specs::jsonb) > 0
                then p.specs::jsonb
                else '[]'::jsonb end as specs
    from shop_product p
),
dim1 as (
    select s.product_id, s.price, s.original_price, s.stock, s.cover,
           coalesce(sp.elem ->> 'name', '规格') as d1name,
           v.val as d1val, v.ord as d1ord
    from spec_src s
    left join lateral (
        select elem from jsonb_array_elements(s.specs) with ordinality as t(elem, idx) where t.idx = 1
    ) sp on true
    left join lateral jsonb_array_elements_text(coalesce(sp.elem -> 'values', '[]'::jsonb))
        with ordinality as v(val, ord) on true
),
dim2 as (
    select s.product_id, sp.elem ->> 'name' as d2name, v.val as d2val, v.ord as d2ord
    from spec_src s
    join lateral (
        select elem from jsonb_array_elements(s.specs) with ordinality as t(elem, idx) where t.idx = 2
    ) sp on true
    join lateral jsonb_array_elements_text(coalesce(sp.elem -> 'values', '[]'::jsonb))
        with ordinality as v(val, ord) on true
),
combined as (
    select d1.product_id, d1.price, d1.original_price, d1.stock, d1.cover,
           coalesce(d1.d1ord, 1) * 100 + coalesce(d2.d2ord, 0) as ord,
           case when d2.d2val is null then d1.d1val
                else d1.d1val || ',' || d2.d2val end as spec_values
    from dim1 d1
    left join dim2 d2 on d2.product_id = d1.product_id
),
numbered as (
    select c.*,
           row_number() over (partition by c.product_id order by c.ord) as i,
           count(*) over (partition by c.product_id) as cnt
    from combined c
)
insert into shop_product_sku (product_id, sku_code, spec_values, image, price, stock)
select n.product_id,
       'SKU-' || n.product_id || '-' || n.i,
       coalesce(nullif(n.spec_values, ''), '标准'),
       n.cover,
       round(n.price + (n.i - 1) * greatest(coalesce(n.original_price, n.price) - n.price, 0) / n.cnt, 2),
       greatest(0, (n.stock / n.cnt)::int)
from numbered n
where coalesce(n.spec_values, '') <> '' or n.cnt = 1;

update shop_product p
set stock = coalesce((select sum(stock) from shop_product_sku s where s.product_id = p.id), p.stock);

-- ---------------------------------------------------------------------
--  Banners & notices
-- ---------------------------------------------------------------------
insert into shop_banner (title, image, link_type, link_value, position, sort, enabled) values
  ('新品首发 · Summit Trail 29', '/images/hero/hero-1.jpg', 'product', '1', 'home', 100, 1),
  ('Enduro 软尾专场 立减 1200', '/images/hero/hero-2.jpg', 'category', '12', 'home', 90, 1),
  ('头盔护具 满 599 减 80', '/images/hero/hero-3.jpg', 'category', '2', 'home', 80, 1),
  ('轮组升级季 碳圈直降', '/images/hero/hero-4.jpg', 'category', '51', 'home', 70, 1);

insert into shop_notice (title, content, sort) values
  ('双十一骑行装备节', '即日起至 11 月 11 日，整车满 3000 减 300，装备满 599 减 80，部分商品支持 12 期免息。', 100),
  ('整车装配与调试说明', '整车出厂已完成 90% 装配，收到后需安装前轮、车把与脚踏。随车附赠扭力扳手与装配指引，建议到店做一次免费复检。', 90);

-- ---------------------------------------------------------------------
--  Coupons
-- ---------------------------------------------------------------------
insert into shop_coupon (name, type, amount, discount, min_amount, total, taken, valid_days, enabled)
values ('新人专享 20 元券', 'amount', 20.00, 10, 99.00, 10000, 128, 7, 1),
       ('装备满 300 减 50', 'amount', 50.00, 10, 300.00, 5000, 342, 15, 1),
       ('整车 9 折券', 'discount', 0.00, 9, 2000.00, 2000, 96, 30, 1);

-- ---------------------------------------------------------------------
--  Demo members
-- ---------------------------------------------------------------------
insert into shop_member (id, mobile, password, nickname, avatar, gender, platform, level, points, balance,
                         total_spent, order_count, created_at)
values (1, '13800138000', '$2a$10$2DCPtAegUj7jNTcRzYMwmuDuRFH1awtaaWQkBH7KWopSmio5TYhre',
        '林小满', '/images/avatars/u1.svg', 2, 'web', 3, 2680, 120.00, 8642.00, 12, now() - interval '180 day'),
       (2, '13800138001', '$2a$10$2DCPtAegUj7jNTcRzYMwmuDuRFH1awtaaWQkBH7KWopSmio5TYhre',
        '周牧', '/images/avatars/u2.svg', 1, 'mp', 2, 940, 0.00, 3120.00, 5, now() - interval '90 day'),
       (3, '13800138002', '$2a$10$2DCPtAegUj7jNTcRzYMwmuDuRFH1awtaaWQkBH7KWopSmio5TYhre',
        'Aria', '/images/avatars/u3.svg', 2, 'web', 1, 120, 0.00, 420.00, 2, now() - interval '30 day'),
       (4, '13800138003', '$2a$10$2DCPtAegUj7jNTcRzYMwmuDuRFH1awtaaWQkBH7KWopSmio5TYhre',
        '陈亦然', '/images/avatars/u4.svg', 1, 'mp', 4, 5230, 300.00, 15680.00, 23, now() - interval '300 day');

-- Explicit ids above never advance the sequence, so push it forward before any
-- insert that relies on the default value.
select setval('shop_member_id_seq', 100);

-- Recent sign-ups so the dashboard's 14 day registration trend has data.
insert into shop_member (mobile, password, nickname, avatar, gender, platform, level, points, balance,
                         total_spent, order_count, created_at)
select
    '139001390' || lpad(d::text, 2, '0'),
    '$2a$10$2DCPtAegUj7jNTcRzYMwmuDuRFH1awtaaWQkBH7KWopSmio5TYhre',
    (array['晓风', '阿沐', '小满', '青柠', '南屿', 'Luca', 'Mia', '若水',
           '一诺', '子墨', '星野', '白露', '拾光', '亦舟'])[d] || d,
    '/images/avatars/u' || (1 + (d % 4)) || '.svg',
    d % 3,
    case when d % 3 = 0 then 'mp' else 'web' end,
    1 + (d % 3),
    (d * 37) % 900,
    0,
    (d * 89) % 2600,
    (d * 7) % 6,
    now() - (d || ' day')::interval
from generate_series(1, 14) as d;

select setval('shop_member_id_seq', 200);

insert into shop_member_address (member_id, receiver, mobile, province, city, district, detail, is_default) values
  (1, '林小满', '13800138000', '浙江省', '杭州市', '西湖区', '文三路 90 号东部软件园 3 幢 801', 1),
  (1, '林小满', '13800138000', '上海市', '上海市', '徐汇区', '漕河泾开发区宜山路 700 号 A 座 1203', 0),
  (2, '周牧', '13800138001', '广东省', '深圳市', '南山区', '科苑路 15 号科兴科学园 B 栋 1802', 1),
  (3, 'Aria', '13800138002', '北京市', '北京市', '朝阳区', '望京 SOHO T1 座 2208', 1),
  (4, '陈亦然', '13800138003', '四川省', '成都市', '武侯区', '天府三街 69 号新希望国际 A 座 1601', 1);

-- ---------------------------------------------------------------------
--  Demo orders spread over the last 30 days for dashboard charts
-- ---------------------------------------------------------------------
insert into shop_order (order_no, member_id, member_nickname, status, total_amount, freight_amount,
                        discount_amount, pay_amount, pay_type, paid_at, receiver, receiver_mobile,
                        receiver_province, receiver_city, receiver_district, receiver_detail,
                        platform, express_company, express_no, shipped_at, finished_at, created_at)
select
    'EC' || to_char(now() - (d || ' day')::interval, 'YYYYMMDD') || lpad(n::text, 5, '0'),
    m.id,
    m.nickname,
    (array[1, 2, 3, 3, 0, 4])[1 + (n % 6)],
    amt,
    0,
    0,
    amt,
    (array['wechat', 'alipay', 'balance', 'mock'])[1 + (n % 4)],
    case when (array[1, 2, 3, 3, 0, 4])[1 + (n % 6)] = 0 then null
         else now() - (d || ' day')::interval + interval '6 minute' end,
    a.receiver, a.mobile, a.province, a.city, a.district, a.detail,
    case when m.platform = 'mp' then 'mp' else 'web' end,
    case when (array[1, 2, 3, 3, 0, 4])[1 + (n % 6)] in (2, 3) then '顺丰速运' end,
    case when (array[1, 2, 3, 3, 0, 4])[1 + (n % 6)] in (2, 3)
         then 'SF' || lpad((n * 137 % 9999999)::text, 10, '0') end,
    case when (array[1, 2, 3, 3, 0, 4])[1 + (n % 6)] in (2, 3)
         then now() - (d || ' day')::interval + interval '1 day' end,
    case when (array[1, 2, 3, 3, 0, 4])[1 + (n % 6)] = 3
         then now() - (d || ' day')::interval + interval '3 day' end,
    now() - (d || ' day')::interval + (n || ' hour')::interval
from generate_series(0, 29) as d
cross join generate_series(1, 3) as n
join shop_member m on m.id = 1 + ((d * 3 + n) % 4)
join lateral (
    select * from shop_member_address ma where ma.member_id = m.id order by ma.is_default desc limit 1
) a on true
cross join lateral (
    select round((120 + (d * 37 + n * 211) % 2600)::numeric, 2) as amt
) t;

insert into shop_order_item (order_no, product_id, sku_id, product_name, product_cover, spec_values, price, quantity, total_amount)
select o.order_no,
       p.id,
       s.id,
       p.name,
       p.cover,
       s.spec_values,
       o.pay_amount,
       1,
       o.pay_amount
from shop_order o
join shop_product p on p.id = 1 + ((o.id * 7) % 20)
join lateral (select * from shop_product_sku sk where sk.product_id = p.id order by sk.id limit 1) s on true
where not exists (select 1 from shop_order_item oi where oi.order_no = o.order_no);

insert into shop_pay_record (order_no, member_id, pay_type, amount, trade_no, status, paid_at)
select order_no, member_id, coalesce(pay_type, 'mock'), pay_amount,
       'TRADE' || lpad((id * 977)::text, 12, '0'), 1, coalesce(paid_at, created_at)
from shop_order
where status in (1, 2, 3);

-- Demo coupons held by members
insert into shop_member_coupon (coupon_id, member_id, status, start_at, end_at)
values (1, 1, 0, now() - interval '2 day', now() + interval '5 day'),
       (2, 1, 0, now() - interval '2 day', now() + interval '13 day'),
       (2, 2, 1, now() - interval '20 day', now() + interval '5 day'),
       (3, 4, 0, now() - interval '1 day', now() + interval '29 day');
