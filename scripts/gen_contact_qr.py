#!/usr/bin/env python3
"""
Generates the QQ / Feishu contact QR placeholders used in README.md.

These are placeholders: they encode a marker string, not a real group link, so
scanning one tells you it is a placeholder instead of silently opening nothing.
Regenerate them with your own target instead of editing the PNG by hand:

    python3 scripts/gen_contact_qr.py \
        --qq "https://qm.qq.com/q/xxxxxxxx" \
        --feishu "https://applink.feishu.cn/client/chat/open?openId=xxxx"

Both values are optional; omitted ones keep the placeholder marker.
"""
import argparse
import pathlib

import qrcode
from PIL import Image, ImageDraw, ImageFont
from qrcode.constants import ERROR_CORRECT_M

OUT_DIR = pathlib.Path(__file__).resolve().parent.parent / "docs" / "images"

# Palette matches the storefront tokens so the blocks do not look pasted in.
INK = (16, 17, 20)
MUTED = (134, 139, 147)
RULE = (226, 226, 220)
QQ_ACCENT = (18, 183, 245)
FEISHU_ACCENT = (51, 112, 255)

SIZE = 480          # final canvas, square
PAD = 32            # white margin around the QR
LABEL_GAP = 22      # space between the code and its caption
LABEL_H = 56        # room reserved for the caption row


def _font(size: int) -> ImageFont.FreeTypeFont:
    """Pick a usable CJK-capable font, else fall back to PIL's default."""
    candidates = [
        "/System/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/Hiragino Sans GB.ttc",
        "/Library/Fonts/Arial Unicode.ttf",
    ]
    for path in candidates:
        if pathlib.Path(path).exists():
            try:
                return ImageFont.truetype(path, size)
            except OSError:
                continue
    return ImageFont.load_default()


def build(target: str, caption: str, accent: tuple, out_name: str) -> pathlib.Path:
    qr = qrcode.QRCode(version=None, error_correction=ERROR_CORRECT_M, box_size=10, border=0)
    qr.add_data(target)
    qr.make(fit=True)
    code = qr.make_image(fill_color=INK, back_color="white").convert("RGB")

    canvas = Image.new("RGB", (SIZE, SIZE), "white")
    draw = ImageDraw.Draw(canvas)

    # rounded outer border
    draw.rounded_rectangle(
        [(0, 0), (SIZE - 1, SIZE - 1)], radius=24, outline=RULE, width=2
    )

    # The caption lives inside the square, so the code has to give up height for
    # it: pad on both sides plus the label block.
    inner = SIZE - PAD * 2 - LABEL_GAP - LABEL_H
    code = code.resize((inner, inner), Image.NEAREST)
    canvas.paste(code, (PAD, PAD))

    label_top = PAD + inner + LABEL_GAP
    draw.rounded_rectangle(
        [(PAD, label_top + 8), (PAD + 6, label_top + 40)], radius=3, fill=accent
    )

    font = _font(30)
    draw.text((PAD + 20, label_top + 6), caption, font=font, fill=INK)

    # A placeholder that scans to a marker string looks like a broken code to
    # anyone who tries it, so say so on the image itself.
    if target.startswith("PLACEHOLDER"):
        badge_font = _font(20)
        text = "占位图 · 待替换"
        box = draw.textbbox((0, 0), text, font=badge_font)
        w, h = box[2] - box[0], box[3] - box[1]
        bx, by = PAD, PAD
        draw.rounded_rectangle(
            [(bx, by), (bx + w + 20, by + h + 14)], radius=6, fill=(255, 244, 238), outline=(240, 67, 26)
        )
        draw.text((bx + 10, by + 5), text, font=badge_font, fill=(214, 58, 18))

    out = OUT_DIR / out_name
    canvas.save(out)
    return out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--qq", default="PLACEHOLDER-QQ-GROUP", help="QQ group link or number")
    parser.add_argument("--feishu", default="PLACEHOLDER-FEISHU-GROUP", help="Feishu group link")
    args = parser.parse_args()

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for target, caption, accent, name in (
        (args.qq, "QQ 交流群", QQ_ACCENT, "contact-qq.png"),
        (args.feishu, "飞书交流群", FEISHU_ACCENT, "contact-feishu.png"),
    ):
        path = build(target, caption, accent, name)
        print(f"{path.relative_to(OUT_DIR.parent.parent)}  <- {target}")


if __name__ == "__main__":
    main()
