#!/usr/bin/env python3
"""Replaces upstream proprietary images under moko-resources/images with Šapat's own:
chat wallpapers (tiled, tinted by the theme), privacy and shield pictures, supporter badges,
the second operator's logos (not shown in Šapat) and the desktop notification icon.
Same names and pixel sizes. Run from the repository root."""
import os, random, math
from playwright.sync_api import sync_playwright
D = "apps/multiplatform/common/src/commonMain/moko-resources/images"
INK = "rgba(36,41,66,0.62)"
BUBBLE = "M36,34h36a8,8 0,0 1,8 8v20a8,8 0,0 1,-8 8H52l-12,10v-10h-4a8,8 0,0 1,-8 -8V42a8,8 0,0 1,8 -8z"
HOLE = "M54,42a6,6 0,0 1,3 11.2V60a3,3 0,0 1,-6 0v-6.8A6,6 0,0 1,54 42z"

def motif(kind, x, y, s, rot):
    t = f'transform="translate({x},{y}) rotate({rot}) scale({s/100})"'
    stroke = f'fill="none" stroke="{INK}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"'
    return {
        "bubble": f'<g {t}><path d="{BUBBLE}" transform="translate(-54,-57)" {stroke}/></g>',
        "key": f'<g {t}><circle cx="-18" cy="0" r="14" {stroke}/><path d="M-4 0 H30 M18 0 V12 M28 0 V9" {stroke}/></g>',
        "lock": f'<g {t}><rect x="-20" y="-6" width="40" height="30" rx="6" {stroke}/><path d="M-12 -6 V-16 a12 12 0 0 1 24 0 V-6" {stroke}/></g>',
        "dot": f'<g {t}><circle r="6" fill="{INK}"/></g>',
        "wave": f'<g {t}><path d="M-30 0 q15 -14 30 0 t30 0" {stroke}/></g>',
        "star": f'<g {t}><path d="M0 -22 L6 -6 L22 -6 L9 4 L14 20 L0 10 L-14 20 L-9 4 L-22 -6 L-6 -6 Z" {stroke}/></g>',
        "leaf": f'<g {t}><path d="M0 24 C-20 4 -14 -18 0 -26 C14 -18 20 4 0 24 Z M0 24 V-18" {stroke}/></g>',
        "plane": f'<g {t}><path d="M-24 4 L24 -14 L4 22 L0 6 Z" {stroke}/></g>',
    }[kind]

SETS = {"wallpaper_school": ["bubble", "key", "lock", "dot"], "wallpaper_cats": ["bubble", "dot", "wave"],
        "wallpaper_flowers": ["leaf", "dot", "bubble"], "wallpaper_hearts": ["bubble", "star", "dot"],
        "wallpaper_kids": ["star", "wave", "dot", "bubble"], "wallpaper_travel": ["plane", "wave", "dot", "bubble"]}

def tile(name, w, h):
    rnd = random.Random(name)
    kinds, parts, pts = SETS[name], [], []
    step = 95
    for gy in range(-1, h // step + 2):
        for gx in range(-1, w // step + 2):
            x = gx * step + rnd.uniform(-22, 22) + (step / 2 if gy % 2 else 0)
            y = gy * step + rnd.uniform(-22, 22)
            k = rnd.choice(kinds)
            s = rnd.uniform(38, 56) if k != "dot" else rnd.uniform(40, 70)
            for dx in (-w, 0, w):           # draw wrapped copies so the tile repeats seamlessly
                for dy in (-h, 0, h):
                    parts.append(motif(k, x + dx, y + dy, s, rnd.uniform(-25, 25)))
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}">{"".join(parts)}</svg>'

def emblem(kind, w, h, acc="#3A5878"):
    if kind == "shield":
        body = f'<path d="M50 5 L92 20 V52 C92 80 72 100 50 108 C28 100 8 80 8 52 V20 Z" fill="{acc}"/><path d="M30 56 L45 71 L72 40" fill="none" stroke="#fff" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/>'
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 100 113">{body}</svg>'
    if kind == "privacy":
        body = f'<rect x="20" y="44" width="60" height="46" rx="9" fill="{acc}"/><path d="M32 44 V32 a18 18 0 0 1 36 0 V44" fill="none" stroke="{acc}" stroke-width="9"/><circle cx="50" cy="64" r="7" fill="#fff"/><rect x="47" y="66" width="6" height="12" rx="3" fill="#fff"/>'
        return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 100 100">{body}</svg>'
    # mark
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="26 30 56 54"><path fill="{acc}" d="{BUBBLE}"/><path fill="#fff" d="{HOLE}"/></svg>'

def main():
    from PIL import Image
    jobs = []
    for f in sorted(os.listdir(D)):
        base = f.split("@")[0].rsplit(".", 1)[0]
        if not f.endswith(".png"):
            continue
        w, h = Image.open(os.path.join(D, f)).size
        if base in SETS:
            jobs.append((f, w, h, tile(base, w, h)))
        elif base in ("privacy", "shield"):
            jobs.append((f, w, h, emblem(base, w, h)))
        elif base.startswith("flux_logo") or base == "icon_foreground_common":
            jobs.append((f, w, h, emblem("mark", min(w, h), min(w, h)).replace('width=', f'x="{(w-min(w,h))//2}" width=', 1) if False else emblem("mark", w, h)))
    with sync_playwright() as p:
        b = p.chromium.launch(executable_path=os.environ.get("CHROME", "/usr/bin/google-chrome"), args=["--no-sandbox"])
        pg = b.new_page()
        for f, w, h, svg in jobs:
            pg.set_viewport_size({"width": w, "height": h})
            pg.set_content(f'<body style="margin:0;background:transparent">{svg}</body>')
            pg.screenshot(path=os.path.join(D, f), omit_background=True, clip={"x": 0, "y": 0, "width": w, "height": h})
        b.close()
    for name, color in (("badge_supporter", "#3A5878"), ("badge_investor", "#5E7FA3"), ("badge_legend", "#14171C")):
        open(os.path.join(D, name + ".svg"), "w").write(
            f'<svg xmlns="http://www.w3.org/2000/svg" width="300" height="399" viewBox="0 0 300 399"><rect x="20" y="20" width="260" height="359" rx="40" fill="{color}"/>'
            f'<g transform="translate(150,200) scale(2.6) translate(-54,-57)"><path fill="#fff" d="{BUBBLE}"/><path fill="{color}" d="{HOLE}"/></g></svg>\n')
    print(len(jobs) + 3, "images replaced")

if __name__ == "__main__":
    main()
