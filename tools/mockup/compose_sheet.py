# -*- coding: utf-8 -*-
"""把 4 张页面截图裁掉底部留白，拼成一张 2x2 总览图。"""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = r"D:\Java\zkxt\docs\mockup"
BG = (245, 247, 251)
SHEET_BG = (238, 242, 248)
PAD = 26

pages = [
    ("page-1-home.png", "首页 · 证书方向与推荐课包"),
    ("page-2-learn.png", "学习页 · 视频 / 目录 / 考点掌握度"),
    ("page-3-exam.png", "考试页 · 答题卡与考点提示"),
    ("page-4-cert.png", "我的证书 · 签发 / 验真 / 到期复证"),
]


def crop_bottom(im, tol=8, margin=30):
    w, h = im.size
    px = im.convert("RGB").load()
    last = h - 1
    for y in range(h - 1, -1, -1):
        row_differs = False
        for x in range(0, w, 6):
            r, g, b = px[x, y]
            if abs(r - BG[0]) > tol or abs(g - BG[1]) > tol or abs(b - BG[2]) > tol:
                row_differs = True
                break
        if row_differs:
            last = y
            break
    return im.crop((0, 0, w, min(h, last + margin)))


imgs = []
for name, _ in pages:
    im = Image.open(os.path.join(OUT, name)).convert("RGB")
    imgs.append(crop_bottom(im))

cell_w = max(i.size[0] for i in imgs)
row_h = [max(imgs[0].size[1], imgs[1].size[1]), max(imgs[2].size[1], imgs[3].size[1])]
print("cell", cell_w, row_h, [i.size for i in imgs])

HEADER = 104
sheet_w = cell_w * 2 + PAD * 3
sheet_h = HEADER + sum(row_h) + PAD * 3 + 34
sheet = Image.new("RGB", (sheet_w, sheet_h), SHEET_BG)
draw = ImageDraw.Draw(sheet)

try:
    f_title = ImageFont.truetype(r"C:\Windows\Fonts\msyhbd.ttc", 34)
    f_sub = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 17)
    f_cap = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 16)
except Exception:
    f_title = f_sub = f_cap = ImageFont.load_default()

draw.text((PAD + 6, 26), "职考云 · 职业技能考证平台 — 前端页面原型", font=f_title, fill=(15, 23, 42))
draw.text((PAD + 8, 68), "4 个核心页面：首页引流 → 学习 → 考试 → 证书（学 · 练 · 考 · 证 闭环）", font=f_sub, fill=(100, 116, 139))

y = HEADER
for row in range(2):
    h = row_h[row]
    for col in range(2):
        idx = row * 2 + col
        name, cap = pages[idx]
        x = PAD + col * (cell_w + PAD)
        cell = Image.new("RGB", (cell_w, h), BG)
        cell.paste(imgs[idx], (0, 0))
        sheet.paste(cell, (x, y))
        draw.rectangle([x - 1, y - 1, x + cell_w, y + h], outline=(219, 226, 236), width=1)
        draw.text((x + 10, y + h + 7), "• " + cap, font=f_cap, fill=(71, 85, 105))
    y += h + PAD + 34

out = os.path.join(OUT, "zhikaoyun-ui-preview.png")
sheet.save(out, "PNG", optimize=True)
print("saved", out, sheet.size, os.path.getsize(out), "bytes")
