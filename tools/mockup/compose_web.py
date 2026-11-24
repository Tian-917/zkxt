# -*- coding: utf-8 -*-
"""把四个真实前端截图拼成 2x2 总览图"""
import io
import os

from PIL import Image, ImageDraw, ImageFont

OUT = r"D:\Java\zkxt\docs\mockup"
BG = (245, 247, 251)
SHEET_BG = (238, 242, 248)
PAD = 26

pages = [
    ("web-home.png", "首页 · 课包列表（ES 真实数据）"),
    ("web-learn.png", "学习页 · 考点目录（course-service）"),
    ("web-exam.png", "答题页 · 组卷（exam-service）"),
    ("web-cert.png", "证书验真（cert-service 公开接口）"),
]


def crop_bottom(im, tol=8, margin=24):
    w, h = im.size
    px = im.convert("RGB").load()
    last = h - 1
    for y in range(h - 1, -1, -1):
        for x in range(0, w, 6):
            r, g, b = px[x, y]
            if abs(r - BG[0]) > tol or abs(g - BG[1]) > tol or abs(b - BG[2]) > tol:
                last = y
                break
        else:
            continue
        break
    return im.crop((0, 0, w, min(h, last + margin)))


imgs = [crop_bottom(Image.open(os.path.join(OUT, n)).convert("RGB")) for n, _ in pages]
cell_w = max(i.size[0] for i in imgs)
row_h = [max(imgs[0].size[1], imgs[1].size[1]), max(imgs[2].size[1], imgs[3].size[1])]

HEADER = 96
sheet_w = cell_w * 2 + PAD * 3
sheet_h = HEADER + sum(row_h) + PAD * 3 + 34
sheet = Image.new("RGB", (sheet_w, sheet_h), SHEET_BG)
draw = ImageDraw.Draw(sheet)

try:
    f_title = ImageFont.truetype(r"C:\Windows\Fonts\msyhbd.ttc", 32)
    f_sub = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 16)
    f_cap = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 15)
except Exception:
    f_title = f_sub = f_cap = ImageFont.load_default()

draw.text((PAD + 6, 22), "职考云 · 前端实跑截图（Vue3 + Vite，接真实微服务接口）",
          font=f_title, fill=(15, 23, 42))
draw.text((PAD + 8, 62), "浏览器 → Vite 代理 → 网关 10010 → 各微服务；数据来自 MySQL / ES / MongoDB",
          font=f_sub, fill=(100, 116, 139))

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

out = os.path.join(OUT, "zhikaoyun-web-preview.png")
sheet.save(out, "PNG", optimize=True)
print("saved", out, sheet.size, os.path.getsize(out))
