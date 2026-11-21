#!/usr/bin/env python3
"""Draws Šapat's own illustrations, under the file names and sizes the app expects.

Writes PNG/JPG files into assets-sapat/multiplatform/resources/MR/images, the folder passed to
Gradle as -Psimplex.assets.dir. None of the upstream (proprietary) artwork is used or traced.
Run from the repository root:  python3 scripts/sapat/gen-illustrations.py
Needs: python3-playwright with a Chromium/Chrome (CHROME env var, default /usr/bin/google-chrome).
"""
import os, random
from playwright.sync_api import sync_playwright

OUT = "assets-sapat/multiplatform/resources/MR/images"
CHROME = os.environ.get("CHROME", "/usr/bin/google-chrome")

THEMES = {  # "_light" files are for the light theme, plain names for the dark theme
    "light": dict(acc="#3A5878", acc2="#5E7FA3", soft="#D7E6F2", soft2="#EEF3F8", ink="#14171C",
                  line="#C3CBD4", card="#FFFFFF", ok="#1F7A4D", hole="#FFFFFF"),
    "dark": dict(acc="#8FB3D6", acc2="#6F93B8", soft="#2B4258", soft2="#1F2B38", ink="#ECEEF1",
                 line="#3A434E", card="#20242A", ok="#5CC28C", hole="#0F1114"),
}

# name: (width, height) at 3x
SIZES = {
    "banner_create_link": (600, 379), "banner_paste_link": (600, 379),
    "card_connect_via_link_alpha": (1260, 785), "card_create_your_public_address_alpha": (1260, 724),
    "card_invite_someone_privately_alpha": (1260, 724), "card_let_someone_connect_to_you_alpha": (1260, 785),
    "connect_via_link": (1200, 653), "connect_via_link_small": (1200, 317),
    "create_channel": (634, 539), "create_group": (634, 539), "create_profile": (634, 539),
    "intro": (1200, 1229), "network_commitments": (1200, 616),
    "one_time_link": (1200, 653), "one_time_link_small": (1200, 317),
    "own_stake": (450, 804), "phone_supporter": (600, 755),
    "simplex_address": (1200, 653), "simplex_address_small": (1200, 426),
    "your_network": (1200, 1433), "your_profile": (1200, 625),
}
JPGS = {"crowdfunding_1": (900, 300), "crowdfunding_2": (900, 658), "crowdfunding_3": (900, 730), "crowdfunding_4": (900, 840)}

BUBBLE = "M36,34h36a8,8 0,0 1,8 8v20a8,8 0,0 1,-8 8H52l-12,10v-10h-4a8,8 0,0 1,-8 -8V42a8,8 0,0 1,8 -8z"
HOLE = "M54,42a6,6 0,0 1,3 11.2V60a3,3 0,0 1,-6 0v-6.8A6,6 0,0 1,54 42z"


def mark(c, x, y, s, fill=None, hole=None):
    """The Šapat bubble-with-keyhole centered at (x, y), s = width."""
    k = s / 52
    return (f'<g transform="translate({x - 54 * k},{y - 57 * k}) scale({k})">'
            f'<path fill="{fill or c["acc"]}" d="{BUBBLE}"/><path fill="{hole or c["hole"]}" d="{HOLE}"/></g>')


def phone(c, x, y, w, h, inner=""):
    r = w * 0.14
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{c["card"]}" stroke="{c["line"]}" stroke-width="{w*0.03}"/>'
            f'<rect x="{x+w*0.36}" y="{y+h*0.035}" width="{w*0.28}" height="{h*0.018}" rx="{h*0.009}" fill="{c["line"]}"/>' + inner)


def msg(c, x, y, w, h, out=True, lock=False):
    fill = c["soft"] if out else c["soft2"]
    r = h * 0.45
    s = f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}"/>'
    s += f'<rect x="{x+h*0.4}" y="{y+h*0.38}" width="{w*0.55}" height="{h*0.12}" rx="{h*0.06}" fill="{c["acc2"]}" opacity=".55"/>'
    if lock:
        lx, ly, ls = x + w - h * 0.75, y + h * 0.28, h * 0.42
        s += (f'<rect x="{lx}" y="{ly+ls*0.42}" width="{ls}" height="{ls*0.62}" rx="{ls*0.12}" fill="{c["acc"]}"/>'
              f'<path d="M{lx+ls*0.22},{ly+ls*0.45}v-{ls*0.12}a{ls*0.28},{ls*0.28} 0 0 1 {ls*0.56},0v{ls*0.12}" '
              f'fill="none" stroke="{c["acc"]}" stroke-width="{ls*0.12}"/>')
    return s


def link(c, x, y, s):
    w = s * 0.62
    return (f'<g fill="none" stroke="{c["acc"]}" stroke-width="{s*0.12}" stroke-linecap="round">'
            f'<rect x="{x}" y="{y}" width="{w}" height="{s*0.38}" rx="{s*0.19}" transform="rotate(-30 {x+w/2} {y+s*0.19})"/>'
            f'<rect x="{x+s*0.38}" y="{y+s*0.22}" width="{w}" height="{s*0.38}" rx="{s*0.19}" transform="rotate(-30 {x+s*0.38+w/2} {y+s*0.41})"/></g>')


def qr(c, x, y, s, seed=7):
    rnd = random.Random(seed)
    n, cell = 13, s / 13
    out = [f'<rect x="{x-cell}" y="{y-cell}" width="{s+2*cell}" height="{s+2*cell}" rx="{cell}" fill="#FFFFFF"/>']
    for i in range(n):
        for j in range(n):
            finder = any(i0 <= i < i0 + 4 and j0 <= j < j0 + 4 for i0, j0 in ((0, 0), (0, 9), (9, 0)))
            on = (i in (0, 3, 9, 12) or j in (0, 3, 9, 12) or (1 <= i <= 2 and 1 <= j <= 2) or (10 <= i <= 11 and 1 <= j <= 2) or (1 <= i <= 2 and 10 <= j <= 11)) if finder else rnd.random() < 0.45
            if on:
                out.append(f'<rect x="{x+j*cell}" y="{y+i*cell}" width="{cell*0.92}" height="{cell*0.92}" fill="#14171C"/>')
    return "".join(out)


def avatar(c, cx, cy, r, letter):
    return (f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{c["acc2"]}"/>'
            f'<text x="{cx}" y="{cy+r*0.36}" font-family="Inter, sans-serif" font-weight="700" font-size="{r*1.0}" text-anchor="middle" fill="#FFFFFF">{letter}</text>')


def node(c, cx, cy, r, main=False):
    return (f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{c["card"]}" stroke="{c["acc"] if main else c["line"]}" stroke-width="{r*0.12}"/>'
            f'<rect x="{cx-r*0.45}" y="{cy-r*0.4}" width="{r*0.9}" height="{r*0.22}" rx="{r*0.06}" fill="{c["acc2"]}"/>'
            f'<rect x="{cx-r*0.45}" y="{cy-r*0.1}" width="{r*0.9}" height="{r*0.22}" rx="{r*0.06}" fill="{c["acc2"]}"/>'
            f'<rect x="{cx-r*0.45}" y="{cy+r*0.2}" width="{r*0.9}" height="{r*0.22}" rx="{r*0.06}" fill="{c["acc2"]}"/>')


def shield(c, cx, cy, s):
    k = s / 100
    return (f'<g transform="translate({cx-50*k},{cy-55*k}) scale({k})"><path d="M50 5 L92 20 V52 C92 80 72 100 50 108 C28 100 8 80 8 52 V20 Z" fill="{c["acc"]}"/>'
            f'<path d="M30 56 L45 71 L72 40" fill="none" stroke="{c["hole"]}" stroke-width="10" stroke-linecap="round" stroke-linejoin="round"/></g>')


def dashed(c, x1, y1, x2, y2, w):
    return f'<path d="M{x1},{y1} L{x2},{y2}" stroke="{c["line"]}" stroke-width="{w}" stroke-dasharray="{w*2.2} {w*1.8}" stroke-linecap="round"/>'


def scene(name, W, H, c):
    s = min(W, H)
    base = name.replace("_alpha", "").replace("card_", "")
    if base == "intro":
        pw, ph = W * 0.3, W * 0.56
        a = phone(c, W*0.08, H*0.24, pw, ph, msg(c, W*0.11, H*0.38, pw*0.75, W*0.07, True, True) + msg(c, W*0.13, H*0.48, pw*0.6, W*0.07, False))
        b = phone(c, W*0.62, H*0.24, pw, ph, msg(c, W*0.65, H*0.38, pw*0.6, W*0.07, False) + msg(c, W*0.66, H*0.48, pw*0.75, W*0.07, True, True))
        return a + b + dashed(c, W*0.4, H*0.42, W*0.6, H*0.42, W*0.012) + mark(c, W*0.5, H*0.13, W*0.18)
    if base == "your_network":
        pts = [(0.5, 0.2), (0.2, 0.42), (0.8, 0.42), (0.32, 0.72), (0.68, 0.72)]
        lines = "".join(dashed(c, W*a[0], H*a[1], W*b[0], H*b[1], W*0.01) for i, a in enumerate(pts) for b in pts[i+1:])
        nodes = "".join(node(c, W*x, H*y, W*0.09, i == 0) for i, (x, y) in enumerate(pts))
        return lines + nodes + mark(c, W*0.5, H*0.9, W*0.16)
    if base == "network_commitments":
        return shield(c, W*0.5, H*0.48, H*0.62) + node(c, W*0.18, H*0.5, H*0.14) + node(c, W*0.82, H*0.5, H*0.14) \
            + dashed(c, W*0.25, H*0.5, W*0.36, H*0.5, H*0.02) + dashed(c, W*0.64, H*0.5, W*0.75, H*0.5, H*0.02)
    if base in ("connect_via_link", "connect_via_link_small", "banner_paste_link", "let_someone_connect_to_you"):
        return link(c, W*0.18, H*0.28, H*0.5) + msg(c, W*0.48, H*0.32, W*0.36, H*0.2, True, True) + mark(c, W*0.82, H*0.72, H*0.22)
    if base in ("one_time_link", "one_time_link_small", "banner_create_link", "invite_someone_privately"):
        return qr(c, W*0.14, H*0.18, H*0.62) + dashed(c, W*0.14 + H*0.75, H*0.5, W*0.62, H*0.5, H*0.025) + mark(c, W*0.76, H*0.5, H*0.42)
    if base in ("simplex_address", "simplex_address_small", "create_your_public_address"):
        card = f'<rect x="{W*0.16}" y="{H*0.2}" width="{W*0.68}" height="{H*0.6}" rx="{H*0.08}" fill="{c["card"]}" stroke="{c["line"]}" stroke-width="{H*0.012}"/>'
        return card + qr(c, W*0.22, H*0.3, H*0.4, 11) + msg(c, W*0.22 + H*0.55, H*0.32, W*0.3, H*0.14, True) \
            + msg(c, W*0.22 + H*0.55, H*0.52, W*0.22, H*0.14, False) + mark(c, W*0.84, H*0.2, H*0.2)
    if base in ("create_profile", "your_profile"):
        return phone(c, W*0.33, H*0.08, W*0.34, H*0.84, avatar(c, W*0.5, H*0.36, W*0.09, "Š")
                     + f'<rect x="{W*0.38}" y="{H*0.55}" width="{W*0.24}" height="{H*0.05}" rx="{H*0.025}" fill="{c["soft"]}"/>'
                     + f'<rect x="{W*0.4}" y="{H*0.64}" width="{W*0.2}" height="{H*0.07}" rx="{H*0.035}" fill="{c["acc"]}"/>')
    if base == "create_group":
        return avatar(c, W*0.3, H*0.45, H*0.13, "A") + avatar(c, W*0.5, H*0.35, H*0.15, "M") + avatar(c, W*0.7, H*0.45, H*0.13, "J") \
            + msg(c, W*0.25, H*0.66, W*0.5, H*0.13, True, True)
    if base == "create_channel":
        return f'<rect x="{W*0.2}" y="{H*0.2}" width="{W*0.6}" height="{H*0.6}" rx="{H*0.06}" fill="{c["card"]}" stroke="{c["line"]}" stroke-width="{H*0.012}"/>' \
            + msg(c, W*0.26, H*0.3, W*0.48, H*0.12, True) + msg(c, W*0.26, H*0.47, W*0.48, H*0.12, True) + msg(c, W*0.26, H*0.64, W*0.34, H*0.12, True) \
            + mark(c, W*0.8, H*0.2, H*0.18)
    # own_stake, phone_supporter: upstream fundraising screens, not shown in Šapat
    return mark(c, W*0.5, H*0.5, s*0.4)


def svg(W, H, body, bg=None):
    back = f'<rect width="{W}" height="{H}" fill="{bg}"/>' if bg else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">{back}{body}</svg>'


def main():
    os.makedirs(OUT, exist_ok=True)
    jobs = []
    for name, (w3, h3) in SIZES.items():
        for theme, suffix in (("dark", ""), ("light", "_light")):
            body = scene(name, w3, h3, THEMES[theme])
            for scale, f in (("3x", 1.0), ("2x", 2 / 3)):
                jobs.append((f"{name}{suffix}@{scale}.png", round(w3 * f), round(h3 * f), svg(w3, h3, body), w3))
    for name, (w3, h3) in JPGS.items():
        for scale, f in (("3x", 1.0), ("2x", 2 / 3)):
            jobs.append((f"{name}@{scale}.jpg", round(w3 * f), round(h3 * f), svg(w3, h3, mark(THEMES["light"], w3/2, h3/2, min(w3, h3)*0.4), "#EEF3F8"), w3))
    with sync_playwright() as p:
        b = p.chromium.launch(executable_path=CHROME, args=["--no-sandbox"])
        pg = b.new_page()
        for fname, w, h, s, w3 in jobs:
            pg.set_viewport_size({"width": w, "height": h})
            pg.set_content(f'<body style="margin:0;background:transparent"><div style="width:{w}px;height:{h}px">'
                           f'{s.replace(f"width=\"{w3}\"", "width=\"100%\"", 1).replace("height=", "data-h=", 1)}</div></body>')
            kw = dict(path=os.path.join(OUT, fname), clip={"x": 0, "y": 0, "width": w, "height": h})
            if fname.endswith(".png"):
                kw["omit_background"] = True
            else:
                kw["type"] = "jpeg"
                kw["quality"] = 88
            pg.screenshot(**kw)
        b.close()
    print(len(jobs), "files in", OUT)


if __name__ == "__main__":
    main()
