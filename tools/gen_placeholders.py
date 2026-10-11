#!/usr/bin/env python3
"""仮画像の生成（docs/art/ASSETS.md の対応表どおりのファイル名）。

    python3 tools/gen_placeholders.py          # 存在しないファイルだけ作る（本番画像は上書きしない）
    python3 tools/gen_placeholders.py --force  # 全部作り直す

本番アセットは core/ui/src/main/res/drawable-nodpi/ の同名ファイルを置き換えるだけで全サーフェスに反映される。
ファイル名を変えないこと（参照は core/ui/.../Assets.kt に一元化）。
仮画像は一目で仮と分かるよう、マゼンタ系の縁と ASCII ラベルを入れてある。
"""
import os, sys
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "core", "ui", "src", "main", "res", "drawable-nodpi")
SCALE = 3  # 1dp = 3px（xxhdpi 相当）で書き出す

# (ファイル名, 幅dp, 高さdp, 種別, ラベル)
RESIDENTS = [  # 番号, ラベル, クラス
    (1, "slime", "M"), (2, "drop", "S"), (3, "pot", "M"), (4, "dragon", "L"), (5, "crystal", "S"),
    (6, "candle", "M"), (7, "guardian", "L"), (8, "mural", "S"), (9, "keys", "S"), (10, "fruit", "M"),
]
CLASS_DP = {"S": 48, "M": 72, "L": 120}
SPEC = []
for n, label, c in RESIDENTS:
    for suffix, kind in (("", "resident"), ("_rare", "rare"), ("_glow", "glow")):
        SPEC.append((f"resident_{n:02d}{suffix}.png", CLASS_DP[c], CLASS_DP[c], kind, f"R{n:02d} {label}{suffix}"))
SPEC += [
    ("prop_lantern.png", 72, 72, "prop", "lantern"),
    ("prop_pot.png", 72, 72, "prop", "pot(sleep)"),
    ("prop_cart.png", 72, 72, "prop", "cart"),
    ("bg_ruins_base_small.png", 110, 110, "bg", "ruins 2x2"),
    ("bg_ruins_base_medium.png", 250, 110, "bg", "ruins 4x2"),
    ("bg_ruins_base_large.png", 250, 180, "bg", "ruins 4x3"),
    ("bg_rare_theme_1.png", 250, 110, "rare_bg", "rare theme 4x2"),
    ("bag_stage_0.png", 48, 48, "prop", "bag0"),
    ("bag_stage_1.png", 48, 48, "prop", "bag1"),
    ("bag_stage_2.png", 48, 48, "prop", "bag2"),
    ("ic_lantern_silhouette.png", 24, 24, "mono", ""),   # 白単色＋透過のみ（smallIcon 要件）
    ("ic_gate_start.png", 24, 24, "icon", "G"),
    ("ic_home_end.png", 24, 24, "icon", "H"),
    ("ic_launcher_fg.png", 108, 108, "launcher", "lantern"),
    ("ic_mana.png", 48, 48, "icon", "M"),                # #23（指示書未作成の追加候補）
]
for w in ("sunny", "cloudy", "rain", "snow"):
    for b in ("day", "evening", "night"):
        SPEC.append((f"sky_{w}_{b}.png", 250, 180, "sky", f"sky {w} {b}"))

SKY = {"day": (120, 180, 230), "evening": (230, 140, 90), "night": (40, 40, 90)}
FILL = {"resident": (110, 200, 190), "rare": (220, 170, 70), "glow": (240, 240, 150), "prop": (180, 140, 100),
        "bg": (120, 110, 100), "rare_bg": (120, 80, 140), "icon": (90, 160, 220), "launcher": (240, 190, 90)}
MARK = (255, 0, 200)


def draw(name, wdp, hdp, kind, label):
    w, h = wdp * SCALE, hdp * SCALE
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if kind == "mono":
        d.rectangle([w * .35, h * .15, w * .65, h * .25], fill=(255, 255, 255, 255))
        d.rounded_rectangle([w * .25, h * .3, w * .75, h * .85], radius=w // 8, fill=(255, 255, 255, 255))
    elif kind == "sky":
        weather, band = name[4:-4].split("_")
        r, g, b = SKY[band]
        if weather in ("cloudy", "rain", "snow"):
            r, g, b = (r + 128) // 2, (g + 128) // 2, (b + 128) // 2
        im = Image.new("RGBA", (w, h), (r, g, b, 110))  # 半透明オーバーレイ
        d = ImageDraw.Draw(im)
        d.text((6, 6), label, fill=(255, 255, 255, 255))
    elif kind in ("bg", "rare_bg"):
        im = Image.new("RGBA", (w, h), FILL[kind] + (255,))
        d = ImageDraw.Draw(im)
        d.rectangle([0, h * .7, w, h], fill=(80, 70, 60, 255))
        d.rectangle([0, 0, w - 1, h - 1], outline=MARK + (255,), width=3)
        d.text((6, 6), "PLACEHOLDER " + label, fill=(255, 255, 255, 255))
    else:
        c = FILL[kind] + (255,)
        d.ellipse([w * .12, h * .25, w * .88, h * .95], fill=c, outline=MARK + (255,), width=max(2, SCALE))
        if kind in ("resident", "rare", "glow"):  # 目（設計原則：目は必須）
            for x in (.38, .62):
                d.ellipse([w * x - w * .06, h * .5, w * x + w * .06, h * .62], fill=(30, 30, 40, 255))
            d.ellipse([w * .25, h * .9, w * .75, h * .98], fill=(60, 40, 80, 90))  # 接地影
        d.text((4, 4), label, fill=(30, 30, 30, 255))
    return im


def main():
    force = "--force" in sys.argv
    os.makedirs(OUT, exist_ok=True)
    made = 0
    for name, wdp, hdp, kind, label in SPEC:
        p = os.path.join(OUT, name)
        if os.path.exists(p) and not force:
            continue
        draw(name, wdp, hdp, kind, label).save(p, optimize=True)
        made += 1
    print(f"{made} files written to {os.path.normpath(OUT)} ({len(SPEC)} in spec)")


if __name__ == "__main__":
    main()
