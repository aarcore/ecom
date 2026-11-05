#!/usr/bin/env python3
"""
Generates the demo image assets referenced by the seed data.

Everything is plain SVG so the repo stays tiny and no binary blobs are needed.
Run:  python3 scripts/gen_assets.py
"""
import os
import random

ROOT = os.path.join(os.path.dirname(__file__), "..", "ecom-boot", "src", "main", "resources", "static")


def write(path: str, body: str) -> None:
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as fh:
        fh.write(body)


# --------------------------------------------------------------------------
#  Palettes: dusty trail tones — sage, clay, slate, sand. Nothing neon; the
#  illustrations should read like parts laid out on a workshop bench.
# --------------------------------------------------------------------------
PALETTES = [
    ("#eef1ec", "#c8d3c4", "#3d5a48", "#ffffff"),
    ("#f6efe9", "#ddc8b6", "#8a5f3e", "#ffffff"),
    ("#eceff3", "#c3cedb", "#3f5670", "#ffffff"),
    ("#f5f1e8", "#ded2b9", "#7d6a3f", "#ffffff"),
    ("#f0eeec", "#d2cbc4", "#5a534c", "#ffffff"),
    ("#edf1f0", "#c2d3d0", "#3d5f5a", "#ffffff"),
    ("#f7f2f4", "#dcc9d0", "#6b4352", "#ffffff"),
    ("#f4f3ee", "#d5d3c4", "#61614a", "#ffffff"),
]

# --------------------------------------------------------------------------
#  Glyphs. Each draws a recognisable piece of mountain-bike kit using simple
#  geometry in a 800x800 viewbox.
# --------------------------------------------------------------------------
W, H = 800, 800


def wheel(cx, cy, r, ink, thick=9):
    """A wheel with rim, hub and four spoke pairs."""
    spokes = []
    for k in range(6):
        ang = k * 30
        spokes.append(
            f'<line x1="{cx}" y1="{cy}" x2="{cx + r*0.86}" y2="{cy}" '
            f'transform="rotate({ang} {cx} {cy})" stroke="{ink}" stroke-width="2.4" opacity="0.5"/>'
        )
    return (
        f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{ink}" stroke-width="{thick}"/>'
        f'<circle cx="{cx}" cy="{cy}" r="{r-16}" fill="none" stroke="{ink}" stroke-width="2" opacity="0.35"/>'
        + "".join(spokes)
        + f'<circle cx="{cx}" cy="{cy}" r="13" fill="{ink}"/>'
        f'<circle cx="{cx}" cy="{cy}" r="5" fill="#fff" opacity="0.85"/>'
    )


def glyph_hardtail(ink):
    """Hardtail MTB in side view: two wheels, frame triangle, fork, bar."""
    front = wheel(238, 540, 150, ink)
    rear = wheel(572, 540, 150, ink)
    frame = (
        # down tube + top tube + seat tube forming the main triangle
        f'<path d="M238 540 L352 372 L470 372 L520 540 Z" fill="none" stroke="{ink}" stroke-width="9" stroke-linejoin="round"/>'
        # seat post + saddle
        f'<path d="M352 372 L330 268" stroke="{ink}" stroke-width="9" stroke-linecap="round"/>'
        f'<path d="M262 258 q68 -22 136 4" stroke="{ink}" stroke-width="13" stroke-linecap="round" fill="none"/>'
        # fork + stem + bar
        f'<path d="M238 540 L300 300" stroke="{ink}" stroke-width="10" stroke-linecap="round"/>'
        f'<path d="M300 300 L372 286" stroke="{ink}" stroke-width="9" stroke-linecap="round"/>'
        f'<path d="M352 276 q28 -10 52 4" stroke="{ink}" stroke-width="10" stroke-linecap="round" fill="none"/>'
        # crank
        f'<circle cx="470" cy="500" r="26" fill="none" stroke="{ink}" stroke-width="7"/>'
        f'<path d="M470 500 L508 552" stroke="{ink}" stroke-width="7" stroke-linecap="round"/>'
    )
    return front + rear + frame


def glyph_full(ink):
    """Full-suspension MTB: adds a shock and swingarm."""
    front = wheel(230, 545, 152, ink)
    rear = wheel(578, 545, 152, ink)
    frame = (
        f'<path d="M230 545 L346 366 L470 366 L516 545 Z" fill="none" stroke="{ink}" stroke-width="9" stroke-linejoin="round"/>'
        # swingarm to pivot
        f'<path d="M578 545 L500 448" stroke="{ink}" stroke-width="11" stroke-linecap="round"/>'
        # shock
        f'<path d="M448 420 L400 352" stroke="{ink}" stroke-width="15" stroke-linecap="round" opacity="0.9"/>'
        f'<path d="M448 420 L400 352" stroke="#fff" stroke-width="5" stroke-linecap="round" opacity="0.5"/>'
        # seat + fork + bar
        f'<path d="M346 366 L326 262" stroke="{ink}" stroke-width="9" stroke-linecap="round"/>'
        f'<path d="M258 252 q68 -22 136 4" stroke="{ink}" stroke-width="13" stroke-linecap="round" fill="none"/>'
        f'<path d="M230 545 L292 296" stroke="{ink}" stroke-width="10" stroke-linecap="round"/>'
        f'<path d="M292 296 L364 282" stroke="{ink}" stroke-width="9" stroke-linecap="round"/>'
        f'<circle cx="470" cy="500" r="24" fill="none" stroke="{ink}" stroke-width="7"/>'
    )
    return front + rear + frame


def glyph_helmet(ink):
    """Half-shell MTB helmet with visor and vent slots."""
    vents = []
    for i in range(4):
        x = 300 + i * 52
        vents.append(
            f'<rect x="{x}" y="330" width="24" height="62" rx="12" fill="#fff" opacity="0.55"/>'
        )
    return (
        f'<path d="M210 470 a180 180 0 0 1 380 0 q0 34 -34 34 h-312 q-34 0 -34 -34 Z" fill="{ink}"/>'
        + "".join(vents)
        + # visor
        f'<path d="M214 452 q96 -74 372 -30 l-22 40 q-206 -31 -330 20 Z" fill="{ink}" opacity="0.82"/>'
        # chin strap
        f'<path d="M262 496 q40 76 132 78 q92 2 132 -78" fill="none" stroke="{ink}" stroke-width="7" opacity="0.55"/>'
        f'<rect x="386" y="556" width="30" height="20" rx="5" fill="{ink}" opacity="0.7"/>'
    )


def glyph_pads(ink):
    """Knee pad: hard shell over a soft sleeve."""
    return (
        f'<path d="M300 210 q100 -26 200 0 l-14 300 q-86 54 -172 0 Z" fill="{ink}" opacity="0.28"/>'
        f'<path d="M292 430 q108 -46 216 0 q14 96 -108 128 q-122 -32 -108 -128 Z" fill="{ink}"/>'
        f'<path d="M330 470 q70 -22 140 0" stroke="#fff" stroke-width="9" fill="none" opacity="0.55" stroke-linecap="round"/>'
        f'<path d="M330 510 q70 -20 140 0" stroke="#fff" stroke-width="9" fill="none" opacity="0.35" stroke-linecap="round"/>'
    )


def glyph_jersey(ink):
    """Short-sleeve cycling jersey with a rear pocket hint."""
    return (
        f'<path d="M330 200 l70 32 l70 -32 l96 56 l-34 96 l-52 -24 v322 h-160 v-322 l-52 24 l-34 -96 Z" fill="{ink}"/>'
        f'<path d="M400 214 q34 30 68 0" stroke="#fff" stroke-width="10" fill="none" opacity="0.5" stroke-linecap="round"/>'
        f'<path d="M348 560 q104 -26 208 0" stroke="#fff" stroke-width="8" fill="none" opacity="0.4" stroke-linecap="round"/>'
        f'<rect x="356" y="520" width="44" height="10" rx="5" fill="#fff" opacity="0.3"/>'
        f'<rect x="404" y="520" width="44" height="10" rx="5" fill="#fff" opacity="0.3"/>'
    )


def glyph_shoe(ink):
    """Clipless MTB shoe in profile."""
    return (
        f'<path d="M196 512 q6 -84 78 -104 l96 -26 q42 -12 86 6 l104 42 q52 22 44 62 l-6 32 h-402 Z" fill="{ink}"/>'
        f'<path d="M196 512 h402 l-4 26 q-2 20 -24 20 h-352 q-22 0 -22 -22 Z" fill="{ink}" opacity="0.7"/>'
        # cleat
        f'<rect x="368" y="540" width="72" height="16" rx="4" fill="#fff" opacity="0.55"/>'
        # laces
        f'<path d="M300 430 l96 22" stroke="#fff" stroke-width="7" opacity="0.5" stroke-linecap="round"/>'
        f'<path d="M306 462 l96 22" stroke="#fff" stroke-width="7" opacity="0.5" stroke-linecap="round"/>'
    )


def glyph_gloves(ink):
    """Pair of half-finger gloves."""
    def glove(x, flip):
        return (
            f'<g transform="translate({x} 300) scale({-1 if flip else 1} 1)">'
            f'<path d="M40 300 q0 -140 96 -140 q96 0 96 140 l0 46 q-96 30 -192 0 Z" fill="{ink}"/>'
            f'<rect x="52" y="120" width="30" height="96" rx="15" fill="{ink}"/>'
            f'<rect x="94" y="108" width="30" height="110" rx="15" fill="{ink}"/>'
            f'<rect x="136" y="118" width="28" height="100" rx="14" fill="{ink}"/>'
            f'<rect x="58" y="286" width="150" height="34" rx="8" fill="#fff" opacity="0.35"/>'
            f'</g>'
        )
    return glove(230, False) + glove(470, True)


def glyph_cassette(ink):
    """Cassette / chainring cluster seen face on."""
    rings = []
    for i, r in enumerate([168, 140, 116, 94, 74, 56]):
        teeth = []
        n = 34 - i * 4
        import math
        for k in range(n):
            a = 2 * math.pi * k / n
            x = 400 + math.cos(a) * r
            y = 400 + math.sin(a) * r
            teeth.append(f'<rect x="{x-4:.1f}" y="{y-4:.1f}" width="8" height="8" fill="{ink}" opacity="0.55"/>')
        rings.append(
            f'<circle cx="400" cy="400" r="{r}" fill="none" stroke="{ink}" stroke-width="6" opacity="{1 - i*0.1:.2f}"/>'
            + "".join(teeth)
        )
    return "".join(rings) + f'<circle cx="400" cy="400" r="34" fill="{ink}"/>'


def glyph_brake(ink):
    """Four-piston caliper with a rotor slice."""
    return (
        f'<circle cx="400" cy="400" r="196" fill="none" stroke="{ink}" stroke-width="10" opacity="0.28"/>'
        + "".join(
            f'<line x1="400" y1="400" x2="{400 + 190}" y2="400" transform="rotate({k*15} 400 400)" stroke="{ink}" stroke-width="3" opacity="0.22"/>'
            for k in range(24)
        )
        + f'<path d="M300 268 h200 q40 0 40 40 v70 h-280 v-70 q0 -40 40 -40 Z" fill="{ink}"/>'
        f'<rect x="262" y="386" width="278" height="26" rx="8" fill="{ink}" opacity="0.65"/>'
        f'<circle cx="352" cy="310" r="16" fill="#fff" opacity="0.6"/>'
        f'<circle cx="448" cy="310" r="16" fill="#fff" opacity="0.6"/>'
    )


def glyph_disc(ink):
    """Standalone floating rotor."""
    import math
    return (
        f'<circle cx="400" cy="400" r="200" fill="none" stroke="{ink}" stroke-width="16"/>'
        f'<circle cx="400" cy="400" r="164" fill="none" stroke="{ink}" stroke-width="5" opacity="0.4"/>'
        + "".join(
            f'<circle cx="{400 + math.cos(2*math.pi*k/6)*130:.1f}" cy="{400 + math.sin(2*math.pi*k/6)*130:.1f}" r="26" fill="none" stroke="{ink}" stroke-width="5" opacity="0.35"/>'
            for k in range(6)
        )
        + f'<circle cx="400" cy="400" r="54" fill="none" stroke="{ink}" stroke-width="9"/>'
        + "".join(
            f'<circle cx="{400 + math.cos(2*math.pi*k/6)*36:.1f}" cy="{400 + math.sin(2*math.pi*k/6)*36:.1f}" r="5" fill="{ink}"/>'
            for k in range(6)
        )
    )


def glyph_wheelset(ink):
    """Pair of wheels side by side."""
    return wheel(252, 420, 176, ink, thick=11) + wheel(556, 420, 176, ink, thick=11)


def glyph_tire(ink):
    """Tyre with knobby tread blocks."""
    import math
    knobs = []
    for k in range(28):
        a = 360 / 28 * k
        knobs.append(
            f'<rect x="386" y="176" width="28" height="42" rx="7" fill="{ink}" opacity="0.6" '
            f'transform="rotate({a} 400 400)"/>'
        )
    return (
        f'<circle cx="400" cy="400" r="224" fill="none" stroke="{ink}" stroke-width="42" opacity="0.24"/>'
        + "".join(knobs)
        + f'<circle cx="400" cy="400" r="150" fill="none" stroke="{ink}" stroke-width="6" opacity="0.35"/>'
    )


def glyph_toolkit(ink):
    """Repair kit: tyre plugs, insertion tool, sealant bottle."""
    return (
        # bottle
        f'<rect x="150" y="330" width="130" height="240" rx="20" fill="{ink}" opacity="0.85"/>'
        f'<rect x="186" y="286" width="58" height="52" rx="10" fill="{ink}"/>'
        f'<rect x="150" y="430" width="130" height="26" fill="#fff" opacity="0.4"/>'
        # tool
        f'<rect x="380" y="300" width="26" height="240" rx="13" fill="{ink}" transform="rotate(12 393 420)"/>'
        f'<path d="M392 292 l44 -46 l26 26 l-44 46 Z" fill="{ink}" opacity="0.75"/>'
        # plugs
        + "".join(
            f'<rect x="{520 + (k%2)*56}" y="{340 + k*36}" width="46" height="16" rx="8" fill="{ink}" opacity="{0.8 - k*0.14:.2f}"/>'
            for k in range(4)
        )
    )


def glyph_fork(ink):
    """Suspension fork: crown, two stanchions, lowers, axle."""
    return (
        # crown
        f'<rect x="278" y="196" width="244" height="52" rx="14" fill="{ink}"/>'
        # steerer
        f'<rect x="382" y="130" width="36" height="76" rx="8" fill="{ink}" opacity="0.8"/>'
        # stanchions
        f'<rect x="302" y="246" width="46" height="230" rx="20" fill="{ink}" opacity="0.55"/>'
        f'<rect x="452" y="246" width="46" height="230" rx="20" fill="{ink}" opacity="0.55"/>'
        # lowers
        f'<path d="M290 420 h86 v190 q0 24 -24 24 h-38 q-24 0 -24 -24 Z" fill="{ink}"/>'
        f'<path d="M424 420 h86 v190 q0 24 -24 24 h-38 q-24 0 -24 -24 Z" fill="{ink}"/>'
        # brace + axle
        f'<rect x="290" y="500" width="220" height="34" rx="12" fill="{ink}" opacity="0.7"/>'
        f'<rect x="300" y="614" width="200" height="22" rx="11" fill="{ink}" opacity="0.5"/>'
    )


def glyph_frame(ink):
    """Bare hardtail frame, no wheels."""
    return (
        f'<path d="M228 520 L352 320 L494 320 L534 520 Z" fill="none" stroke="{ink}" stroke-width="16" stroke-linejoin="round"/>'
        f'<path d="M352 320 L330 214" stroke="{ink}" stroke-width="15" stroke-linecap="round"/>'
        f'<path d="M228 520 L296 268" stroke="{ink}" stroke-width="17" stroke-linecap="round"/>'
        f'<path d="M278 268 L392 254" stroke="{ink}" stroke-width="15" stroke-linecap="round"/>'
        # dropouts
        f'<circle cx="228" cy="520" r="22" fill="none" stroke="{ink}" stroke-width="12"/>'
        f'<circle cx="534" cy="520" r="22" fill="none" stroke="{ink}" stroke-width="12"/>'
        f'<circle cx="352" cy="214" r="20" fill="none" stroke="{ink}" stroke-width="11"/>'
        # bottle bosses
        f'<circle cx="430" cy="372" r="7" fill="{ink}" opacity="0.5"/>'
        f'<circle cx="466" cy="452" r="7" fill="{ink}" opacity="0.5"/>'
    )


def glyph_shock(ink):
    """Air rear shock: body, shaft, eyelets."""
    return (
        f'<rect x="330" y="300" width="140" height="200" rx="26" fill="{ink}"/>'
        f'<rect x="330" y="330" width="140" height="28" fill="#fff" opacity="0.35"/>'
        f'<path d="M400 300 L400 150" stroke="{ink}" stroke-width="30" stroke-linecap="round"/>'
        f'<circle cx="400" cy="140" r="34" fill="none" stroke="{ink}" stroke-width="14"/>'
        f'<path d="M400 500 L400 640" stroke="{ink}" stroke-width="24" stroke-linecap="round"/>'
        f'<circle cx="400" cy="652" r="34" fill="none" stroke="{ink}" stroke-width="14"/>'
        # damping dial
        f'<circle cx="470" cy="400" r="24" fill="none" stroke="#fff" stroke-width="7" opacity="0.6"/>'
    )


GLYPHS = {
    "hardtail": glyph_hardtail,
    "full": glyph_full,
    "helmet": glyph_helmet,
    "pads": glyph_pads,
    "jersey": glyph_jersey,
    "shoe": glyph_shoe,
    "gloves": glyph_gloves,
    "cassette": glyph_cassette,
    "brake": glyph_brake,
    "disc": glyph_disc,
    "wheelset": glyph_wheelset,
    "tire": glyph_tire,
    "toolkit": glyph_toolkit,
    "fork": glyph_fork,
    "frame": glyph_frame,
    "shock": glyph_shock,
}

# slug, glyph, label shown inside the illustration
PRODUCTS = [
    ("p1", "hardtail", "TRAIL 29"),
    ("p1b", "hardtail", "TRAIL 29"),
    ("p1c", "hardtail", "TRAIL 29"),
    ("p2", "hardtail", "XC 200"),
    ("p2b", "hardtail", "XC 200"),
    ("p3", "full", "ENDURO 170"),
    ("p3b", "full", "ENDURO 170"),
    ("p4", "full", "AM 150"),
    ("p4b", "full", "AM 150"),
    ("p5", "helmet", "AIRFLOW"),
    ("p5b", "helmet", "AIRFLOW"),
    ("p6", "helmet", "HALF SHELL"),
    ("p7", "pads", "PAD SET"),
    ("p7b", "pads", "PAD SET"),
    ("p8", "pads", "KNEE"),
    ("p9", "jersey", "JERSEY"),
    ("p9b", "jersey", "JERSEY"),
    ("p10", "jersey", "VEST"),
    ("p11", "shoe", "CLIP SHOE"),
    ("p11b", "shoe", "CLIP SHOE"),
    ("p12", "gloves", "GLOVES"),
    ("p13", "cassette", "12-SPEED"),
    ("p13b", "cassette", "12-SPEED"),
    ("p14", "cassette", "11-SPEED"),
    ("p15", "brake", "4-PISTON"),
    ("p15b", "brake", "4-PISTON"),
    ("p16", "disc", "ROTOR 203"),
    ("p17", "wheelset", "CARBON 29"),
    ("p17b", "wheelset", "CARBON 29"),
    ("p18", "wheelset", "ALLOY 29"),
    ("p19", "tire", "TYRE 2.4"),
    ("p19b", "tire", "TYRE 2.4"),
    ("p20", "toolkit", "REPAIR KIT"),
    ("p21", "fork", "FORK 160"),
    ("p21b", "fork", "FORK 160"),
    ("p22", "fork", "FORK 130"),
    ("p23", "frame", "FRAME 29"),
    ("p23b", "frame", "FRAME 29"),
    ("p24", "shock", "SHOCK"),
]


def product_svg(shape: str, label: str, palette, seed: int) -> str:
    bg, ring, ink, light = palette
    rnd = random.Random(seed)
    blobs = []
    for _ in range(3):
        cx = rnd.randint(60, 740)
        cy = rnd.randint(60, 740)
        r = rnd.randint(90, 200)
        blobs.append(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{ring}" opacity="0.28"/>')
    glyph = GLYPHS[shape](ink)
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" role="img" aria-label="{label}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{bg}"/>
      <stop offset="100%" stop-color="{ring}" stop-opacity="0.55"/>
    </linearGradient>
  </defs>
  <rect width="{W}" height="{H}" fill="url(#bg)"/>
  {''.join(blobs)}
  <circle cx="{W/2}" cy="{H*0.47}" r="{W*0.29}" fill="#ffffff" opacity="0.5"/>
  {glyph}
  <text x="{W/2}" y="{H*0.925}" text-anchor="middle" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="{W*0.038}" font-weight="600" letter-spacing="{W*0.006}" fill="{ink}" opacity="0.72">{label}</text>
</svg>
"""


for index, (slug, shape, label) in enumerate(PRODUCTS):
    palette = PALETTES[index % len(PALETTES)]
    write(f"images/products/{slug}.svg", product_svg(shape, label, palette, index))
print(f"products: {len(PRODUCTS)}")

# --------------------------------------------------------------------------
#  Banners: wide gradient panels with an editorial headline.
# --------------------------------------------------------------------------
BANNERS = [
    (
        "b1",
        "新品首发",
        "SUMMIT TRAIL 29",
        "液压成型车架 · 12 速单盘 · 100mm 气压前叉",
        ("#1e2b22", "#3f6349"),
        "#eef4ee",
    ),
    (
        "b2",
        "ENDURO 专场",
        "立减 ¥1200",
        "170mm 行程 · 四连杆避震 · 碳纤维前三角",
        ("#2a2119", "#6b4a2e"),
        "#f6efe4",
    ),
    (
        "b3",
        "头盔护具",
        "满 ¥599 减 ¥80",
        "MIPS 旋转防护 · D3O 缓震 · 全场通用",
        ("#1b2634", "#2f4a63"),
        "#e8f0f7",
    ),
    (
        "b4",
        "轮组升级季",
        "碳圈直降",
        "29 寸 · 28mm 内宽 · 84T 啮合花鼓",
        ("#2c2431", "#5c4468"),
        "#f2ecf5",
    ),
]

for slug, kicker, title, sub, (c1, c2), text in BANNERS:
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 620" width="1200" height="620" role="img" aria-label="{title}">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
    <radialGradient id="glow" cx="0.78" cy="0.24" r="0.62">
      <stop offset="0%" stop-color="#ffffff" stop-opacity="0.3"/>
      <stop offset="100%" stop-color="#ffffff" stop-opacity="0"/>
    </radialGradient>
  </defs>
  <rect width="1200" height="620" fill="url(#g)"/>
  <rect width="1200" height="620" fill="url(#glow)"/>
  <g opacity="0.13" stroke="{text}" stroke-width="1.2">
    <path d="M0 470 L1200 300"/><path d="M0 560 L1200 390"/>
  </g>
  <circle cx="960" cy="250" r="196" fill="#ffffff" opacity="0.09"/>
  <circle cx="1010" cy="300" r="128" fill="#ffffff" opacity="0.11"/>
  <text x="86" y="196" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="30" font-weight="600" letter-spacing="6" fill="{text}" opacity="0.78">{kicker}</text>
  <text x="86" y="316" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="76" font-weight="700" letter-spacing="-2" fill="{text}">{title}</text>
  <text x="88" y="386" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="27" fill="{text}" opacity="0.82">{sub}</text>
  <rect x="88" y="432" width="182" height="56" rx="28" fill="{text}"/>
  <text x="179" y="468" text-anchor="middle" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="22" font-weight="650" fill="{c1}">立即查看</text>
</svg>
"""
    write(f"images/banners/{slug}.svg", svg)
print(f"banners: {len(BANNERS)}")

# --------------------------------------------------------------------------
#  Category banners: a narrow strip per top level category.
# --------------------------------------------------------------------------
CATS = [
    ("cat-bike", "山地整车", "#eef1ec", "#3d5a48", "🚵"),
    ("cat-helmet", "头盔护具", "#1b2634", "#2f4a63", "🪖"),
    ("cat-apparel", "骑行服饰", "#f7f2f4", "#6b4352", "👕"),
    ("cat-drivetrain", "传动制动", "#f5f1e8", "#7d6a3f", "⚙️"),
    ("cat-wheel", "轮组轮胎", "#eceff3", "#3f5670", "🛞"),
    ("cat-frame", "避震车架", "#f0eeec", "#5a534c", "🔧"),
]
for slug, name, bg, ink, glyph in CATS:
    # dark palettes need light text so the label stays readable
    dark = bg.startswith("#1") or bg.startswith("#2")
    text_color = "#eef4ee" if dark else ink
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 750 240" width="750" height="240" role="img" aria-label="{name}">
  <rect width="750" height="240" fill="{bg}"/>
  <circle cx="640" cy="60" r="120" fill="{'#ffffff' if dark else ink}" opacity="0.10"/>
  <circle cx="700" cy="200" r="80" fill="{'#ffffff' if dark else ink}" opacity="0.08"/>
  <text x="56" y="112" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="46" font-weight="700" fill="{text_color}">{name}</text>
  <text x="58" y="162" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="21" fill="{text_color}" opacity="0.7">精选装备 · 越野可靠</text>
  <text x="600" y="168" font-size="96" text-anchor="middle">{glyph}</text>
</svg>
"""
    write(f"images/banners/{slug}.svg", svg)
print(f"category banners: {len(CATS)}")

# --------------------------------------------------------------------------
#  Brand marks: rounded squares with initials.
# --------------------------------------------------------------------------
BRANDS = [
    ("summit", "S", "#3d5a48", "#eef1ec"),
    ("ridgeline", "R", "#3f5670", "#eceff3"),
    ("torque", "T", "#8a5f3e", "#f6efe9"),
    ("crest", "C", "#6b4352", "#f7f2f4"),
    ("trailhead", "T", "#61614a", "#f4f3ee"),
]
for slug, letter, ink, bg in BRANDS:
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200" role="img" aria-label="{slug}">
  <rect width="200" height="200" rx="52" fill="{bg}"/>
  <text x="100" y="134" text-anchor="middle" font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif" font-size="92" font-weight="700" fill="{ink}">{letter}</text>
</svg>
"""
    write(f"images/brands/{slug}.svg", svg)
print(f"brands: {len(BRANDS)}")

AVATARS = [
    ("admin", "#3d5a48", "#eef1ec"),
    ("operator", "#3f5670", "#eceff3"),
    ("u1", "#8a5f3e", "#f6efe9"),
    ("u2", "#3f5670", "#eceff3"),
    ("u3", "#6b4352", "#f7f2f4"),
    ("u4", "#7d6a3f", "#f5f1e8"),
]
for slug, ink, bg in AVATARS:
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200" role="img" aria-label="avatar">
  <circle cx="100" cy="100" r="100" fill="{bg}"/>
  <circle cx="100" cy="82" r="34" fill="{ink}" opacity="0.9"/>
  <path d="M40 200 a60 60 0 0 1 120 0 Z" fill="{ink}" opacity="0.9"/>
</svg>
"""
    write(f"images/avatars/{slug}.svg", svg)
print(f"avatars: {len(AVATARS)}")

# Remove assets from the previous (electronics) demo so the repo carries no
# orphaned images after the theme switch.
LEGACY = [
    "images/brands/nova.svg",
    "images/brands/kiri.svg",
    "images/brands/nordic.svg",
    "images/brands/peak.svg",
    "images/brands/pure.svg",
    "images/banners/cat-digital.svg",
    "images/banners/cat-computer.svg",
    "images/banners/cat-home.svg",
    "images/banners/cat-fashion.svg",
    "images/banners/cat-food.svg",
    "images/banners/cat-beauty.svg",
]
removed = 0
for rel in LEGACY:
    full = os.path.join(ROOT, rel)
    if os.path.exists(full):
        os.remove(full)
        removed += 1
print(f"removed legacy assets: {removed}")


# --------------------------------------------------------------------------
#  Masthead hero art. These are full-bleed 1920x1080 panels, much larger and
#  more atmospheric than the product tiles: a dark gradient ground, a large
#  silhouetted bike or component, and a warm rim light from the lower right.
# --------------------------------------------------------------------------
HEROES = [
    ("hero-1", "hardtail", ("#0d1110", "#1d3a2c", "#63d29a"), "TRAIL 29"),
    ("hero-2", "full", ("#120f0d", "#3d1f14", "#ff7a3d"), "ENDURO 170"),
    ("hero-3", "helmet", ("#0c1016", "#152c40", "#5aa9e6"), "AIRFLOW MIPS"),
    ("hero-4", "wheelset", ("#100e14", "#2b1f3d", "#a97cf0"), "CARBON 29"),
]

HERO_W, HERO_H = 1920, 1080


def hero_svg(shape: str, label: str, colors, seed: int) -> str:
    ground, mid, rim = colors
    rnd = random.Random(seed * 31 + 7)

    # ridge silhouettes give the panel depth without competing with the product
    ridges = []
    for layer in range(3):
        points = []
        base = HERO_H * (0.58 + layer * 0.12)
        amp = 130 - layer * 34
        for i in range(0, 13):
            x = HERO_W / 12 * i
            y = base - rnd.uniform(-amp, amp)
            points.append(f"{x:.0f},{y:.0f}")
        ridges.append(
            f'<polygon points="0,{HERO_H} {" ".join(points)} {HERO_W},{HERO_H}" '
            f'fill="{mid}" opacity="{0.18 + layer * 0.1:.2f}"/>'
        )

    # scale the component glyph up and place it right of centre
    glyph = GLYPHS[shape](rim)
    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {HERO_W} {HERO_H}" width="{HERO_W}" height="{HERO_H}" role="img" aria-label="{label}">
  <defs>
    <linearGradient id="sky" x1="0" y1="0" x2="0.6" y2="1">
      <stop offset="0%" stop-color="{ground}"/>
      <stop offset="100%" stop-color="{mid}"/>
    </linearGradient>
    <radialGradient id="rim" cx="0.72" cy="0.62" r="0.5">
      <stop offset="0%" stop-color="{rim}" stop-opacity="0.42"/>
      <stop offset="100%" stop-color="{rim}" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="fade" x1="0" y1="0" x2="1" y2="0">
      <stop offset="0%" stop-color="{ground}" stop-opacity="0.82"/>
      <stop offset="55%" stop-color="{ground}" stop-opacity="0"/>
    </linearGradient>
  </defs>
  <rect width="{HERO_W}" height="{HERO_H}" fill="url(#sky)"/>
  {''.join(ridges)}
  <rect width="{HERO_W}" height="{HERO_H}" fill="url(#rim)"/>
  <g transform="translate(1010 196) scale(1.02)" opacity="0.96">{glyph}</g>
  <rect width="{HERO_W}" height="{HERO_H}" fill="url(#fade)"/>
  <text x="{HERO_W - 90}" y="{HERO_H - 76}" text-anchor="end"
        font-family="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', sans-serif"
        font-size="132" font-weight="700" letter-spacing="10"
        fill="{rim}" opacity="0.14">{label}</text>
</svg>
"""


for index, (slug, shape, colors, label) in enumerate(HEROES):
    write(f"images/hero/{slug}.svg", hero_svg(shape, label, colors, index))
print(f"heroes: {len(HEROES)}")
