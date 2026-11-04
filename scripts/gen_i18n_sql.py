#!/usr/bin/env python3
"""Generates V3__i18n_text.sql from a compact in-script dictionary.

Kept in the repo so the demo translations can be regenerated or extended when
the seed catalogue changes, rather than hand-editing 400 INSERT rows.
"""

import pathlib

OUT = pathlib.Path(__file__).resolve().parents[1] / \
    'ecom-boot/src/main/resources/db/migration/V3__i18n_text.sql'


def q(text: str) -> str:
    return "'" + text.replace("'", "''") + "'"


rows = []          # (entity, entity_id, field, lang, value)
terms = []         # (source, lang, value)

# ---------------------------------------------------------------- categories
CATEGORIES = {
    1: ('Mountain Bikes', 'マウンテンバイク'),
    2: ('Helmets & Protection', 'ヘルメット・プロテクション'),
    3: ('Riding Apparel', 'ウェア'),
    4: ('Drivetrain & Brakes', 'ドライブトレイン・ブレーキ'),
    5: ('Wheels & Tyres', 'ホイール・タイヤ'),
    6: ('Suspension & Frames', 'サスペンション・フレーム'),
    11: ('Hardtail Bikes', 'ハードテイル'),
    12: ('Full Suspension', 'フルサスペンション'),
    21: ('Helmets', 'ヘルメット'),
    22: ('Knee & Elbow Pads', 'ニー・エルボーパッド'),
    31: ('Jerseys', 'ジャージ'),
    32: ('Shoes & Gloves', 'シューズ・グローブ'),
    41: ('Drivetrain Groupsets', 'コンポーネント'),
    42: ('Brakes & Rotors', 'ブレーキ・ローター'),
    51: ('Wheelsets', 'ホイールセット'),
    52: ('Tyres & Tubes', 'タイヤ・チューブ'),
    61: ('Forks', 'フォーク'),
    62: ('Frames & Shocks', 'フレーム・リアショック'),
}
for cid, (en, ja) in CATEGORIES.items():
    rows.append(('category', cid, 'name', 'en', en))
    rows.append(('category', cid, 'name', 'ja', ja))

# -------------------------------------------------------------------- brands
BRANDS = {
    1: ('Race-level mountain bikes built around lightweight frames',
        '軽量フレームを軸としたレース向けマウンテンバイク'),
    2: ('Hardtails and XC builds with a value-first spec',
        'コストパフォーマンス重視のハードテイルとXCモデル'),
    3: ('Suspension and drivetrain engineering for hard terrain',
        '過酷な路面に特化したサスペンションとドライブトレイン'),
    4: ('Wheelsets, helmets and protective gear',
        'ホイール・ヘルメット・プロテクション'),
    5: ('Apparel, footwear and ride bags',
        'ウェア・シューズ・ライドバッグ'),
}
for bid, (en, ja) in BRANDS.items():
    rows.append(('brand', bid, 'description', 'en', en))
    rows.append(('brand', bid, 'description', 'ja', ja))

# ------------------------------------------------------------------ products
# name_en, name_ja, subtitle_en, subtitle_ja, detail_en, detail_ja
PRODUCTS = {
    1: ('Summit Trail 29 Hardtail', 'Summit Trail 29 ハードテイル',
        '29" wheels · Alloy frame · 12-speed · 100mm fork',
        '29インチ · アルミフレーム · 12速 · 100mmフォーク',
        'The Trail 29 is Summit\'s workhorse hardtail for everyday trail riding and light singletrack. The 6061 alloy frame uses hydroformed tubing with a stiff bottom bracket and room for 2.4" rubber in the rear triangle. A 100mm air fork and 12-speed single-ring drivetrain cover a wide enough gear range that new riders can settle into long climbs.',
        'Trail 29は、日常のトレイル走行と軽いシングルトラックのためのSummitの主力ハードテイル。6061アルミフレームはハイドロフォーミング成形でBB剛性が高く、リア三角は2.4インチタイヤに対応。100mmエアフォークと12速シングルリングで、登りが続いても余裕のあるギア比を確保しています。'),
    2: ('Ridge Line XC 200 Hardtail', 'Ridge Line XC 200 ハードテイル',
        'XC geometry · Hydraulic discs · Air fork · Entry-level racing',
        'XCジオメトリ · 油圧ディスク · エアフォーク · レース入門向け',
        'The XC 200 runs a racier 69° head angle and short chainstays for quick steering. It ships with hydraulic disc brakes and 2.25" tyres, which suits a mix of race courses and city-edge singletrack.',
        'XC 200はレース寄りの69°ヘッドアングルと短いチェーンステーで、素早いハンドリングを実現。油圧ディスクブレーキと2.25インチタイヤを標準装備し、レースコースと街郊外のシングルトラックの混合路に適しています。'),
    3: ('Summit Enduro 170 Full Suspension', 'Summit Enduro 170 フルサスペンション',
        '170mm travel · Four-bar linkage · Carbon front triangle',
        '170mmトラベル · 4バーリンク · カーボンフロントトライアングル',
        'The Enduro 170 is built for long-travel trail riding and enduro racing. The four-bar linkage balances pedalling support with absorption, and 170mm front and rear paired with a progressive spring curve gives plenty of support on landings. The carbon front triangle keeps the complete bike at 14.2kg.',
        'Enduro 170はロングトラベルトレイルとエンデューロレース向け。4バーリンクがペダリングの支えと衝撃吸収のバランスを取り、前後170mmとプログレッシブなスプリングカーブが着地時のサポートを確保します。カーボンフロントトライアングルにより完成車重量は14.2kgに収まっています。'),
    4: ('Torque Lab AM 150 Full Suspension', 'Torque Lab AM 150 フルサスペンション',
        '150mm travel · Alloy frame · All-mountain geometry',
        '150mmトラベル · アルミフレーム · オールマウンテンジオメトリ',
        'The AM 150 is the answer to "one bike for everything". A 65° head angle keeps it stable on descents while still climbing efficiently, and 150mm covers the vast majority of trail riding. Bottle cage mounts and tool tabs make it a good long-distance companion.',
        'AM 150は「1台ですべてをこなす」ための答え。65°のヘッドアングルが下りの安定性と登りの効率を両立し、150mmトラベルはほとんどのトレイルをカバーします。ボトルケージ台座とツールマウントを備え、長距離走行にも向いています。'),
    5: ('Crest Airflow MIPS Helmet', 'Crest Airflow MIPS ヘルメット',
        'MIPS rotational protection · 18 vents · Adjustable visor',
        'MIPS回転保護 · 18ベント · 調整式バイザー',
        'The Airflow uses a MIPS low-friction layer to reduce rotational acceleration on angled impacts. Eighteen vents paired with internal channelling keep air moving even on slow climbs. The rear dial adjusts one-handed.',
        'AirflowはMIPS低摩擦レイヤーを採用し、斜めからの衝撃時の回転加速度を低減します。18のベントと内部チャネルにより、低速の登りでも通気を維持。後頭部のダイヤルは片手で調整できます。'),
    6: ('Crest Half Shell Helmet', 'Crest ハーフシェルヘルメット',
        '280g · 12 vents · XC and city-edge trails',
        '280g · 12ベント · XCと街郊外のトレイル向け',
        'The Half Shell targets lightweight trail riding: 280g stays comfortable over a long day. Rear coverage extends 3cm lower than a road helmet, balancing protection with airflow.',
        'Half Shellは軽量トレイル向け。280gで長時間の装着も快適です。後頭部のカバーはロードヘルメットより3cm低く、保護と通気のバランスを取っています。'),
    7: ('Trailhead Knee & Elbow Pad Set', 'Trailhead ニー・エルボーパッドセット',
        'D3O padding · Silicone gripper · Breathable mesh',
        'D3Oパッド · シリコングリッパー · メッシュ素材',
        'The set includes one pair of knee pads and one pair of elbow pads. D3O stays soft in normal use and hardens instantly on impact to spread the load. Silicone strips top and bottom stop the pads wandering on long descents.',
        'ニーパッドとエルボーパッドが各1組入ったセット。D3Oは通常時は柔らかく、衝撃時に瞬時に硬化して荷重を分散します。上下のシリコンストリップが長い下りでもパッドのずれを防ぎます。'),
    8: ('Trailhead Lightweight Knee Pads', 'Trailhead 軽量ニーパッド',
        '120g each · Perforated padding · Everyday trail use',
        '片側120g · パンチングパッド · 日常のトレイル向け',
        'For pedal-heavy trail riding, these knee pads use perforated EVA padding that trades a little coverage for heat management. Elastic straps hold them without pressing on the vessels behind the knee.',
        'ペダリングの多いトレイル向けに、パンチング加工のEVAパッドを採用し、多少の保護範囲と引き換えに放熱性を高めました。伸縮ストラップが膝裏の血管を圧迫せずに固定します。'),
    9: ('Trailhead Quick-Dry Jersey', 'Trailhead 速乾ジャージ',
        'Four-way stretch · Dropped tail · Rear zip pocket',
        '4ウェイストレッチ · 長めの後裾 · 背面ジップポケット',
        'The front panel uses a high-airflow mesh while the back is treated to wick faster. The tail drops 4cm so it still covers your lower back in the riding position. Three rear pockets take a phone and a couple of gels.',
        '前面は通気性の高いメッシュ、背面は速乾加工で汗を素早く排出。後裾は4cm長く、ライディングポジションでも腰回りをカバーします。背面3ポケットにはスマホとジェルを収納できます。'),
    10: ('Trailhead Wind Vest', 'Trailhead ウィンドベスト',
        'Lightweight windproof · Packs into rear pocket · Reflective details',
        '軽量防風 · 背面ポケットに収納 · リフレクター',
        'Stuff it into a rear pocket on the climb, pull it on before the descent. At 96g the fabric blocks wind without being fully waterproof, which suits mountain weather that swings through the day.',
        '登りでは背面ポケットに収納し、下りの前に羽織るだけ。96gの生地は防風性を備えつつ完全防水ではありません。山の気温差が大きい環境に適しています。'),
    11: ('Trailhead Clipless MTB Shoes', 'Trailhead クリップレスMTBシューズ',
        'Carbon composite sole · Adjustable studs · Water-repellent upper',
        'カーボン複合ソール · 調整式スタッド · 撥水上甲',
        'The carbon composite midsole is stiff enough for pedalling but still flexes when you have to push the bike. Two adjustable studs up front bite into soft ground, and the upper has a water-repellent finish.',
        'カーボン複合ミッドソールはペダリングに十分な剛性を持ちながら、押し歩き時のしなりも確保。前足部の調整式スタッド2本が軟らかい路面に食い込み、アッパーは撥水加工です。'),
    12: ('Trailhead Cushioned Gloves', 'Trailhead クッショングローブ',
        'Palm padding · Half finger · Touchscreen fingertips',
        '掌パッド · ハーフフィンガー · タッチスクリーン対応',
        'Silicone palm pads spread pressure on long descents, while the half-finger cut keeps shifting crisp. Thumb and index finger work with touchscreens so you never have to take them off.',
        'シリコンの掌パッドが長い下りでの圧迫を分散し、ハーフフィンガーがシフト操作の繊細さを保ちます。親指と人差し指はタッチスクリーン対応で、外す必要がありません。'),
    13: ('Torque Lab 12-Speed Groupset', 'Torque Lab 12速コンポーネント',
        '12-speed · 10-51T cassette · Single ring · Shifter and derailleur',
        '12速 · 10-51Tカセット · シングルリング · シフターとディレイラー',
        'The kit includes shifter, derailleur, cassette and chain. A 510% gear range covers steep climbs and flat sprints, and the derailleur clutch keeps the chain on over rough ground.',
        'シフター、ディレイラー、カセット、チェーンを含むキット。510%のギアレンジが急坂と平坦なスプリントをカバーし、ディレイラーのクラッチが荒れた路面でもチェーン外れを防ぎます。'),
    14: ('Ridge Line 11-Speed Shifter & Derailleur', 'Ridge Line 11速シフター＆ディレイラー',
        '11-speed · Long cage · Fits 46T cassettes',
        '11速 · ロングケージ · 46Tカセット対応',
        'A common first upgrade. The long-cage derailleur takes cassettes up to 46T, and the dual-release shifter lets you drop several gears at once so you can commit to a climb.',
        '最初のアップグレードとして定番。ロングケージのディレイラーは最大46Tのカセットに対応し、デュアルリリースのシフターは一度に複数段のダウンシフトが可能。登りで迷わず踏み込めます。'),
    15: ('Torque Lab 4-Piston Hydraulic Disc Brakes', 'Torque Lab 4ポッド油圧ディスクブレーキ',
        'Four-piston caliper · Ceramic pistons · Rotor and pads included',
        '4ポッドキャリパー · セラミックピストン · ローターとパッド付属',
        'Four pistons resist fade far better than two on long descents. Ceramic pistons insulate heat and reduce the risk of boiling the fluid. Shipped pre-bled and ready to bolt on.',
        '4ポッドは長い下りでのフェードに対して2ポッドよりはるかに強い耐久性を持ちます。セラミックピストンは熱を遮断し、フルードの沸騰リスクを低減。エア抜き済みで届きます。'),
    16: ('Torque Lab 203mm Floating Rotor', 'Torque Lab 203mm フローティングローター',
        '203mm · Floating design · Six-bolt mount',
        '203mm · フローティング構造 · 6ボルト固定',
        'A floating rotor lets the inner and outer rings expand at different rates, so it resists warping under heat. At 203mm it delivers noticeably more braking torque, which suits long descents and heavier riders.',
        'フローティング構造により内外リングの熱膨張差を吸収し、高温でも歪みにくくなります。203mmは制動トルクが明確に大きく、長い下りや体重の重いライダーに適しています。'),
    17: ('Crest Carbon 29 Wheelset', 'Crest Carbon 29 ホイールセット',
        '29" · Carbon rim · 28mm internal · Tubeless ready',
        '29インチ · カーボンリム · 内幅28mm · チューブレスレディ',
        'A 28mm internal width supports a more stable tyre profile at 2.3-2.5" pressures. The pair weighs 1580g and transfers power from the bottom bracket directly. Hubs run six pawls with 84T engagement.',
        '内幅28mmが2.3〜2.5インチの空気圧でより安定したタイヤ形状を支えます。ペア重量1580gでBBからの力をダイレクトに伝達。ハブは6爪・84Tの噛合です。'),
    18: ('Crest Alloy 29 Wheelset', 'Crest Alloy 29 ホイールセット',
        '29" · Alloy rim · 25mm internal · Tubeless ready',
        '29インチ · アルミリム · 内幅25mm · チューブレスレディ',
        'A value-focused alloy wheelset. The 25mm internal width suits 2.2-2.4" tyres, and it ships with tubeless tape and valves already fitted so you can convert without buying extras.',
        'コスト重視のアルミホイールセット。内幅25mmは2.2〜2.4インチタイヤに適合し、チューブレステープとバルブが装着済みのため追加購入なしでチューブレス化できます。'),
    19: ('Crest 29x2.4 Tubeless Tyre', 'Crest 29x2.4 チューブレスタイヤ',
        '29 x 2.4 · Dual-ply casing · Medium-soft compound',
        '29 x 2.4 · デュアルプライ · ミディアムソフトコンパウンド',
        'A 120TPI dual-ply casing balances puncture resistance with trail feel. The medium-soft compound holds on wet roots and loose rock, with a low-rolling centre tread and supportive shoulder lugs.',
        '120TPIのデュアルプライが耐パンク性と路面感覚を両立。ミディアムソフトコンパウンドは濡れた根やルーズな岩場でグリップし、センターは低転がり、ショルダーはコーナーを支えます。'),
    20: ('Crest Tubeless Repair Kit', 'Crest チューブレスリペアキット',
        'Plugs · Insertion tool · 60ml sealant',
        'プラグ · 挿入ツール · シーラント60ml',
        'A field fix for a punctured tubeless tyre. The rubber plugs and insertion tool seal a hole in about two minutes without removing the wheel, and the sealant top-up handles routine maintenance.',
        'チューブレスタイヤのパンクに対する現場修理キット。ゴムプラグと挿入ツールでホイールを外さず約2分で穴を塞げます。シーラントの補充用も日常メンテナンスに使えます。'),
    21: ('Torque Lab 160mm Air Fork', 'Torque Lab 160mm エアフォーク',
        '160mm travel · 35mm stanchions · Air spring · Adjustable damping',
        '160mmトラベル · 35mmスタンチオン · エアスプリング · 減衰調整',
        '35mm stanchions hold torsional rigidity better at long travel. The air spring adjusts pressure and rebound, and low-speed compression has three settings for everything from gravel to repeated drops.',
        '35mmスタンチオンはロングトラベルでもねじり剛性を維持します。エアスプリングは空気圧とリバウンドを調整でき、低速圧縮は3段階。砂利から連続したドロップまで対応します。'),
    22: ('Torque Lab 130mm Trail Fork', 'Torque Lab 130mm トレイルフォーク',
        '130mm travel · 34mm stanchions · Lightweight build',
        '130mmトラベル · 34mmスタンチオン · 軽量設計',
        'Aimed at short-travel trail bikes. 130mm with 34mm stanchions saves 320g over the 160mm version, which suits routes where pedalling efficiency matters more than descending composure.',
        'ショートトラベルトレイルバイク向け。130mmと34mmスタンチオンの組み合わせは160mmモデルより320g軽く、下りの安定性よりペダリング効率を重視するルートに適しています。'),
    23: ('Ridge Line Alloy Hardtail Frame', 'Ridge Line アルミハードテイルフレーム',
        '6061 alloy · 29" · Boost spacing · Headset included',
        '6061アルミ · 29インチ · Boost開幅 · ヘッドセット付属',
        'A frame sold on its own for custom builds or replacements. Hydroformed tubes are butted thicker at the bottom bracket and head tube, and Boost 148 rear spacing fits current wheelsets. Headset and seat clamp included.',
        'カスタムビルドや載せ替え用に単体販売されるフレーム。ハイドロフォーミング管はBBとヘッドチューブで厚く成形され、Boost 148のリア開幅は現行ホイールに対応。ヘッドセットとシートクランプ付属。'),
    24: ('Torque Lab Rear Shock 205x65', 'Torque Lab リアショック 205x65',
        '205x65mm · Air · Three-position compression · Lockout',
        '205x65mm · エア · 3段階圧縮 · ロックアウト',
        'Fits 150-170mm travel frames. Three compression positions let you firm it up for the climb and open it fully for the descent, and the pedalling platform has a lockout lever.',
        '150〜170mmトラベルのフレームに適合。3段階の圧縮設定で登りでは固く、下りでは全開にできます。ペダリングプラットフォームにはロックアウトレバー付き。'),
}
for pid, (n_en, n_ja, s_en, s_ja, d_en, d_ja) in PRODUCTS.items():
    rows.append(('product', pid, 'name', 'en', n_en))
    rows.append(('product', pid, 'name', 'ja', n_ja))
    rows.append(('product', pid, 'subtitle', 'en', s_en))
    rows.append(('product', pid, 'subtitle', 'ja', s_ja))
    rows.append(('product', pid, 'detail', 'en', d_en))
    rows.append(('product', pid, 'detail', 'ja', d_ja))

# ------------------------------------------------------------------- coupons
COUPONS = {
    1: ('New member ¥20 off', '新規会員 20元クーポン'),
    2: ('¥50 off gear over ¥300', '300元以上の装備で50元OFF'),
    3: ('10% off complete bikes', '完成車10%OFF'),
}
for cid, (en, ja) in COUPONS.items():
    rows.append(('coupon', cid, 'name', 'en', en))
    rows.append(('coupon', cid, 'name', 'ja', ja))

# ------------------------------------------------------------------- banners
BANNERS = {
    1: ('New arrival · Summit Trail 29', '新発売 · Summit Trail 29'),
    2: ('Enduro season · ¥1200 off', 'エンデューロ特集 · 1200元OFF'),
    3: ('Helmets · ¥80 off over ¥599', 'ヘルメット · 599元以上で80元OFF'),
    4: ('Wheel upgrade season · Carbon on sale', 'ホイールアップグレード季 · カーボン割引'),
}
for bid, (en, ja) in BANNERS.items():
    rows.append(('banner', bid, 'title', 'en', en))
    rows.append(('banner', bid, 'title', 'ja', ja))

# ------------------------------------------------------------------- notices
rows += [
    ('notice', 1, 'title', 'en', 'Autumn gear festival'),
    ('notice', 1, 'title', 'ja', '秋のギアフェスティバル'),
    ('notice', 1, 'content', 'en',
     'Until 11 November: ¥300 off complete bikes over ¥3000, ¥80 off gear over ¥599, and interest-free instalments on selected items.'),
    ('notice', 1, 'content', 'ja',
     '11月11日まで：3000元以上の完成車で300元OFF、599元以上の装備で80元OFF、一部商品は分割手数料無料。'),
    ('notice', 2, 'title', 'en', 'Bike assembly and setup'),
    ('notice', 2, 'title', 'ja', '完成車の組み立てと調整'),
    ('notice', 2, 'content', 'en',
     'Complete bikes ship 90% assembled. Fit the front wheel, handlebar and pedals on arrival. A torque wrench and assembly guide are in the box; we recommend a free check-over at your local shop.'),
    ('notice', 2, 'content', 'ja',
     '完成車は90%組み立てた状態で出荷されます。到着後に前輪・ハンドル・ペダルを取り付けてください。トルクレンチと組立ガイドが同梱されています。ショップでの無料点検をおすすめします。'),
]

# --------------------------------------------------- vocabulary (term table)
# Small closed vocabularies: spec dimension names, common spec values and units.
TERMS = {
    '车架尺码': ('Frame size', 'フレームサイズ'),
    '尺码': ('Size', 'サイズ'),
    '颜色': ('Colour', 'カラー'),
    '涂装': ('Finish', '塗装'),
    '版本': ('Version', '仕様'),
    '安装标准': ('Mount standard', 'マウント規格'),
    '直径': ('Diameter', '直径'),
    '花鼓': ('Hub', 'ハブ'),
    '规格': ('Size', '規格'),
    '胎体': ('Casing', 'ケーシング'),
    '行程': ('Travel', 'トラベル'),
    '舵管': ('Steerer', 'ステアラー'),

    '哑光黑': ('Matte black', 'マットブラック'),
    '林道绿': ('Trail green', 'トレイルグリーン'),
    '珍珠白': ('Pearl white', 'パールホワイト'),
    '赛道蓝': ('Race blue', 'レースブルー'),
    '碳黑': ('Carbon black', 'カーボンブラック'),
    '熔岩橙': ('Lava orange', 'ラバオレンジ'),
    '哑光石墨': ('Matte graphite', 'マットグラファイト'),
    '军绿': ('Army green', 'アーミーグリーン'),
    '云白': ('Cloud white', 'クラウドホワイト'),
    '荧光绿': ('Neon green', 'ネオングリーン'),
    '黑': ('Black', 'ブラック'),
    '白': ('White', 'ホワイト'),
    '灰': ('Grey', 'グレー'),
    '苔绿': ('Moss green', 'モスグリーン'),
    '石墨灰': ('Graphite grey', 'グラファイトグレー'),
    '砖红': ('Brick red', 'ブリックレッド'),
    '荧光黄': ('Neon yellow', 'ネオンイエロー'),
    '沙棕': ('Sand brown', 'サンドブラウン'),
    '森林绿': ('Forest green', 'フォレストグリーン'),
    '标准': ('Standard', 'スタンダード'),
    '含曲柄': ('With crankset', 'クランク付き'),
    '不含曲柄': ('Without crankset', 'クランクなし'),
    '右侧单拨': ('Right shifter only', '右シフターのみ'),
    '左右套装': ('Left and right pair', '左右セット'),
    'IS 转接': ('IS adaptor', 'ISアダプター'),
    '锥形舵管': ('Tapered steerer', 'テーパードステアラー'),
    '直管': ('Straight steerer', 'ストレートステアラー'),
    '六钉/Boost': ('6-bolt / Boost', '6ボルト / Boost'),
    '中锁/Boost': ('Centerlock / Boost', 'センターロック / Boost'),
    '60TPI 防刺': ('60TPI puncture resistant', '60TPI 耐パンク'),

    '台': ('bike', '台'),
    '顶': ('helmet', '個'),
    '套': ('set', 'セット'),
    '只': ('pad', '個'),
    '件': ('item', '点'),
    '双': ('pair', '足'),
    '片': ('rotor', '枚'),
    '对': ('pair', '組'),
    '条': ('tyre', '本'),
    '支': ('unit', '本'),
    '个': ('unit', '個'),
}
for source, (en, ja) in TERMS.items():
    terms.append((source, 'en', en))
    terms.append((source, 'ja', ja))


def build() -> str:
    head = """-- =====================================================================
--  Content translations (en / ja)
--
--  Chinese stays in the source tables: every column, every seeded row and
--  every default label is already zh-CN, so a demo reset never depends on
--  this overlay. Only the other languages live here.
--
--  Two shapes:
--    shop_i18n_text  keyed by (entity, entity_id, field, lang) for long form
--                    content — product names, details, category labels.
--    shop_i18n_term  keyed by the source string itself, for small closed
--                    vocabularies where an id would be meaningless: spec
--                    dimension names, spec values and units.
--
--  Generated by scripts/gen_i18n_sql.py — edit that, not this file.
-- =====================================================================
create table shop_i18n_text
(
    id         bigserial primary key,
    entity     varchar(32) not null, -- product | category | brand | coupon | banner | notice
    entity_id  bigint      not null,
    field      varchar(32) not null, -- name | subtitle | detail | description | title | content
    lang       varchar(8)  not null, -- en | ja  (zh-CN lives in the source tables)
    value      text        not null,
    created_at timestamp   not null default now(),
    updated_at timestamp   not null default now()
);

-- upserts from the admin console target this key
create unique index uk_i18n_text on shop_i18n_text (entity, entity_id, field, lang);

-- batch lookups always filter by language first, then by entity keys
create index idx_i18n_text_lang_entity on shop_i18n_text (lang, entity, entity_id);

create table shop_i18n_term
(
    id         bigserial primary key,
    source     varchar(64) not null, -- the zh-CN text as stored in the data
    lang       varchar(8)  not null,
    value      varchar(255) not null,
    created_at timestamp   not null default now()
);

create unique index uk_i18n_term on shop_i18n_term (source, lang);

"""
    lines = [head]
    lines.append('insert into shop_i18n_text (entity, entity_id, field, lang, value) values\n')
    lines.append(',\n'.join(
        f'  ({q(e)}, {i}, {q(f)}, {q(l)}, {q(v)})' for e, i, f, l, v in rows))
    lines.append(';\n\n')
    lines.append('insert into shop_i18n_term (source, lang, value) values\n')
    lines.append(',\n'.join(f'  ({q(s)}, {q(l)}, {q(v)})' for s, l, v in terms))
    lines.append(';\n')
    return ''.join(lines)


OUT.write_text(build())
print(f'wrote {OUT} — {len(rows)} text rows, {len(terms)} terms')
