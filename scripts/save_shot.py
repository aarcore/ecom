#!/usr/bin/env python3
"""
Turns a raw CDP Page.captureScreenshot response into a README image.

The agent drives the browser; this handles the bytes so the output size is
exactly the viewport (no rescaling, no blank strip) and the file size stays
reasonable.

    python3 scripts/save_shot.py <cdp-response.json> <name.png|name.jpg> [width]

`width` downscales only, never upscales; it defaults to keeping the capture
size. JPEG is used for the photo-heavy hero shots, PNG for UI.
"""
import base64
import io
import json
import pathlib
import sys

from PIL import Image

OUT = pathlib.Path(__file__).resolve().parent.parent / "docs" / "images"


def load_png(path: str) -> Image.Image:
    payload = json.loads(pathlib.Path(path).read_text())
    data = payload.get("data") or payload.get("result", {}).get("data")
    if not data:
        raise SystemExit(f"no screenshot data in {path}")
    return Image.open(io.BytesIO(base64.b64decode(data))).convert("RGB")


def main() -> None:
    if len(sys.argv) < 3:
        raise SystemExit(__doc__)
    src, name = sys.argv[1], sys.argv[2]
    target_w = int(sys.argv[3]) if len(sys.argv) > 3 else None

    im = load_png(src)

    # Trim a trailing all-white column run, which is the visual symptom of a
    # viewport/canvas mismatch. Never trims into content: only identical white
    # columns at the very edge are dropped.
    px = im.load()
    right = im.width - 1
    while right > 0:
        column_white = True
        for y in range(0, im.height, 7):
            r, g, b = px[right, y]
            if not (r > 250 and g > 250 and b > 250):
                column_white = False
                break
        if not column_white:
            break
        right -= 1
    if right < im.width - 1:
        keep = right + 1
        # Only act on a meaningful strip, and keep a 1px safety margin.
        if im.width - keep > 8:
            im = im.crop((0, 0, keep, im.height))

    if target_w and target_w < im.width:
        h = round(im.height * target_w / im.width)
        im = im.resize((target_w, h), Image.LANCZOS)

    OUT.mkdir(parents=True, exist_ok=True)
    dest = OUT / name
    if dest.suffix.lower() in (".jpg", ".jpeg"):
        im.save(dest, quality=84, optimize=True, progressive=True)
    else:
        im.save(dest, optimize=True)
    print(f"{dest.relative_to(OUT.parent.parent)}  {im.size}  {dest.stat().st_size // 1024}K")


if __name__ == "__main__":
    main()
