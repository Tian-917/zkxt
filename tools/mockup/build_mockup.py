# -*- coding: utf-8 -*-
"""生成「职考云」前端高保真原型页面（静态 HTML，无外部资源），供 Chrome 无头渲染截图。"""
import io
import os
import random

OUT = r"D:\Java\zkxt\docs\mockup"
os.makedirs(OUT, exist_ok=True)

CSS = r"""
:root{
  --brand:#2563eb; --brand-2:#4f46e5; --brand-soft:#eef2ff;
  --gold:#b45309; --gold-2:#f59e0b; --gold-soft:#fffbeb;
  --ok:#059669; --ok-soft:#ecfdf5; --warn:#d97706;
  --ink:#0f172a; --ink-2:#334155; --muted:#64748b; --muted-2:#94a3b8;
  --line:#e6ebf4; --bg:#f5f7fb; --card:#ffffff;
  --shadow:0 1px 2px rgba(15,23,42,.04), 0 8px 24px -12px rgba(15,23,42,.12);
}
*{box-sizing:border-box;margin:0;padding:0}
html,body{height:100%}
body{
  background:var(--bg); color:var(--ink);
  font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","Microsoft YaHei","PingFang SC",sans-serif;
  -webkit-font-smoothing:antialiased; overflow:hidden;
}
.page{width:1440px;height:960px;overflow:hidden;position:relative}
/* ---------- flow strip ---------- */
.flow{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:22px}
.fstep{position:relative;padding:14px 16px;border-radius:13px;background:#fff;border:1px solid var(--line);
  box-shadow:var(--shadow);display:flex;gap:11px;align-items:center}
.fstep .n{width:26px;height:26px;border-radius:50%;background:var(--brand-soft);color:var(--brand);
  font-size:12px;font-weight:700;display:flex;align-items:center;justify-content:center;flex:none}
.fstep h4{font-size:12.5px}
.fstep p{font-size:11px;color:var(--muted);margin-top:2px}
.fstep:after{content:"›";position:absolute;right:-11px;top:50%;transform:translateY(-50%);
  color:#cbd5e1;font-size:17px;z-index:2}
.fstep:last-child:after{display:none}
/* ---------- stats strip ---------- */
.stats-strip{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:18px}
.mstat{padding:14px 16px;border-radius:13px;background:#fff;border:1px solid var(--line);box-shadow:var(--shadow)}
.mstat b{font-size:20px;letter-spacing:.4px}
.mstat b em{font-size:11px;font-style:normal;color:var(--muted);margin-left:3px}
.mstat span{display:block;font-size:11px;color:var(--muted);margin-top:3px}
/* ---------- exam extra ---------- */
.pointrow{display:grid;grid-template-columns:1fr 320px;gap:18px;margin-top:16px}
.tagline{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}
.chip{font-size:11px;color:var(--brand);background:var(--brand-soft);border:1px solid #dbe4ff;
  border-radius:7px;padding:4px 9px}
/* ---------- next certs ---------- */
.next{display:grid;grid-template-columns:repeat(3,1fr);gap:14px;margin-top:8px}
.nc{display:flex;gap:12px;align-items:center;padding:14px 16px;border-radius:13px;background:#fff;
  border:1px solid var(--line);box-shadow:var(--shadow)}
.nc .ic{width:36px;height:36px;border-radius:10px;background:var(--brand-soft);color:var(--brand);
  display:flex;align-items:center;justify-content:center;flex:none}
.nc h4{font-size:13px}
.nc p{font-size:11px;color:var(--muted);margin-top:2px}
.nc .go{margin-left:auto;font-size:11.5px;color:var(--brand);font-weight:600}
.wrap{width:1280px;margin:0 auto}
/* ---------- nav ---------- */
.nav{height:64px;background:rgba(255,255,255,.9);border-bottom:1px solid var(--line);
  display:flex;align-items:center;position:sticky;top:0;z-index:9}
.nav .wrap{display:flex;align-items:center;gap:28px}
.logo{display:flex;align-items:center;gap:10px}
.logo .mark{width:34px;height:34px;border-radius:10px;background:linear-gradient(135deg,#2563eb,#4f46e5);
  color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:17px;
  box-shadow:0 6px 14px -6px rgba(37,99,235,.7)}
.logo b{font-size:17px;letter-spacing:.3px}
.logo .beta{font-size:10px;color:var(--brand);background:var(--brand-soft);border-radius:5px;padding:2px 6px;font-weight:600}
.menu{display:flex;gap:22px;font-size:14px;color:var(--ink-2)}
.menu a{text-decoration:none;color:inherit;padding:6px 0;position:relative}
.menu a.on{color:var(--brand);font-weight:600}
.menu a.on:after{content:"";position:absolute;left:0;right:0;bottom:-19px;height:2px;background:var(--brand);border-radius:2px}
.search{margin-left:auto;display:flex;align-items:center;gap:8px;background:#f1f5f9;border:1px solid var(--line);
  border-radius:10px;padding:8px 12px;width:260px;color:var(--muted);font-size:13px}
.avatar{width:34px;height:34px;border-radius:50%;background:linear-gradient(135deg,#fbbf24,#f97316);
  color:#fff;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:600}
.iconbtn{width:34px;height:34px;border-radius:10px;border:1px solid var(--line);display:flex;
  align-items:center;justify-content:center;color:var(--muted);background:#fff}
/* ---------- generic ---------- */
.sec-title{display:flex;align-items:baseline;gap:10px;margin:0 0 14px}
.sec-title h2{font-size:18px;letter-spacing:.2px}
.sec-title span{font-size:12px;color:var(--muted-2)}
.sec-title .more{margin-left:auto;font-size:12px;color:var(--muted)}
.card{background:var(--card);border:1px solid var(--line);border-radius:14px;box-shadow:var(--shadow)}
.badge{font-size:11px;border-radius:6px;padding:2px 7px;font-weight:600}
.badge.blue{background:var(--brand-soft);color:var(--brand)}
.badge.green{background:var(--ok-soft);color:var(--ok)}
.badge.gold{background:var(--gold-soft);color:var(--gold)}
.btn{border:none;border-radius:10px;padding:11px 20px;font-size:14px;font-weight:600;cursor:pointer;
  font-family:inherit}
.btn.primary{background:linear-gradient(135deg,#2563eb,#4f46e5);color:#fff;box-shadow:0 10px 20px -12px rgba(37,99,235,.9)}
.btn.ghost{background:#fff;color:var(--ink-2);border:1px solid var(--line)}
.muted{color:var(--muted)}
.tiny{font-size:12px}
.bar{height:6px;border-radius:99px;background:#eef2f7;overflow:hidden}
.bar>i{display:block;height:100%;border-radius:99px;background:linear-gradient(90deg,#2563eb,#60a5fa)}
/* ---------- hero ---------- */
.hero{margin-top:22px;border-radius:18px;overflow:hidden;position:relative;
  background:linear-gradient(120deg,#1e3a8a 0%,#2563eb 45%,#4f46e5 100%);color:#fff;
  display:flex;align-items:center;padding:30px 38px;gap:30px}
.hero:before{content:"";position:absolute;right:-80px;top:-120px;width:420px;height:420px;border-radius:50%;
  background:radial-gradient(circle,rgba(255,255,255,.22),transparent 62%)}
.hero:after{content:"";position:absolute;right:180px;bottom:-160px;width:320px;height:320px;border-radius:50%;
  background:radial-gradient(circle,rgba(255,255,255,.12),transparent 65%)}
.hero .copy{position:relative;z-index:1;flex:1}
.hero .eyebrow{font-size:12px;letter-spacing:1px;opacity:.86;margin-bottom:10px}
.hero h1{font-size:30px;line-height:1.28;letter-spacing:.4px}
.hero p{font-size:13px;opacity:.82;margin-top:10px}
.hero .actions{display:flex;gap:12px;margin-top:20px}
.hero .actions .btn.primary{background:#fff;color:#1e3a8a;box-shadow:0 12px 24px -14px rgba(0,0,0,.6)}
.hero .actions .btn.line{background:rgba(255,255,255,.12);color:#fff;border:1px solid rgba(255,255,255,.34)}
.hero .stats{position:relative;z-index:1;display:flex;gap:12px}
.stat{width:118px;padding:14px 14px;border-radius:14px;background:rgba(255,255,255,.13);
  border:1px solid rgba(255,255,255,.24);backdrop-filter:blur(6px)}
.stat b{display:block;font-size:22px;letter-spacing:.5px}
.stat span{font-size:11px;opacity:.84}
/* ---------- cert directions ---------- */
.dirs{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:26px}
.dir{padding:16px;border-radius:14px;background:#fff;border:1px solid var(--line);box-shadow:var(--shadow);
  display:flex;gap:12px;align-items:flex-start}
.dir .ic{width:38px;height:38px;border-radius:11px;display:flex;align-items:center;justify-content:center;
  background:var(--brand-soft);color:var(--brand);flex:none}
.dir .ic.g{background:var(--gold-soft);color:var(--gold)}
.dir .ic.gn{background:var(--ok-soft);color:var(--ok)}
.dir h3{font-size:14px;margin-bottom:4px}
.dir p{font-size:11.5px;color:var(--muted);line-height:1.5}
.dir .foot{margin-top:8px;font-size:11px;color:var(--muted-2);display:flex;gap:8px}
/* ---------- course cards ---------- */
.courses{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-top:8px}
.course{border-radius:16px;overflow:hidden;background:#fff;border:1px solid var(--line);box-shadow:var(--shadow)}
.cover{height:118px;position:relative;display:flex;align-items:flex-end;padding:12px;color:#fff}
.cover.c1{background:linear-gradient(135deg,#1d4ed8,#3b82f6 60%,#60a5fa)}
.cover.c2{background:linear-gradient(135deg,#0f766e,#14b8a6 60%,#5eead4)}
.cover.c3{background:linear-gradient(135deg,#7c3aed,#a855f7 60%,#c084fc)}
.cover .tag{position:absolute;top:12px;left:12px;font-size:11px;padding:3px 8px;border-radius:6px;
  background:rgba(255,255,255,.22);border:1px solid rgba(255,255,255,.32)}
.cover .t{font-size:15px;font-weight:600;letter-spacing:.3px}
.cbody{padding:14px}
.cbody h3{font-size:14px;margin-bottom:8px;line-height:1.45}
.meta{display:flex;gap:14px;font-size:11.5px;color:var(--muted);margin-bottom:12px}
.prog{display:flex;align-items:center;gap:10px;font-size:11px;color:var(--muted)}
.prog .bar{flex:1}
.price{display:flex;align-items:baseline;gap:6px;margin-top:12px}
.price b{font-size:19px;color:#e11d48}
.price s{font-size:11px;color:var(--muted-2)}
.price .go{margin-left:auto;font-size:12px;color:var(--brand);font-weight:600}
/* ---------- learn page ---------- */
.crumb{margin:18px 0 12px;font-size:12.5px;color:var(--muted);display:flex;gap:8px;align-items:center}
.crumb b{color:var(--ink-2);font-weight:600}
.learn{display:grid;grid-template-columns:252px 1fr 296px;gap:16px;align-items:start}
.side{padding:14px;max-height:760px;overflow:hidden}
.side h4{font-size:12px;color:var(--muted);letter-spacing:.6px;margin-bottom:10px}
.chap{display:flex;align-items:center;gap:8px;padding:8px 6px;font-size:13px;color:var(--ink-2);border-radius:8px}
.chap b{font-size:13px}
.chap .n{margin-left:auto;font-size:11px;color:var(--muted-2)}
.lesson{display:flex;align-items:center;gap:8px;padding:8px 8px 8px 22px;font-size:12.5px;color:var(--ink-2);
  border-radius:8px;position:relative}
.lesson.on{background:var(--brand-soft);color:var(--brand);font-weight:600}
.lesson .du{margin-left:auto;font-size:11px;color:var(--muted-2)}
.lesson.done .dot{background:var(--ok)}
.lesson .dot{width:7px;height:7px;border-radius:50%;background:#cbd5e1;flex:none}
.player{border-radius:16px;overflow:hidden;border:1px solid var(--line);background:#0b1220}
.stage{height:392px;position:relative;background:
  radial-gradient(circle at 30% 30%,rgba(37,99,235,.5),transparent 55%),
  radial-gradient(circle at 72% 66%,rgba(79,70,229,.45),transparent 58%),
  linear-gradient(135deg,#0b1220,#111c33 60%,#0b1220)}
.stage .play{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:74px;height:74px;border-radius:50%;
  background:rgba(255,255,255,.92);display:flex;align-items:center;justify-content:center;
  box-shadow:0 18px 40px -16px rgba(0,0,0,.7)}
.stage .play i{width:0;height:0;border-left:20px solid #1e3a8a;border-top:12px solid transparent;
  border-bottom:12px solid transparent;margin-left:5px}
.stage .ttl{position:absolute;left:18px;top:16px;color:#fff;font-size:13px;opacity:.9}
.stage .ttl small{display:block;font-size:11px;opacity:.62;margin-top:3px}
.bar-ctl{height:52px;display:flex;align-items:center;gap:12px;padding:0 16px;background:#0f1729;color:#cbd5e1}
.bar-ctl .track{flex:1;height:4px;border-radius:99px;background:#27324a;position:relative}
.bar-ctl .track i{position:absolute;left:0;top:0;bottom:0;width:42%;border-radius:99px;background:#3b82f6}
.bar-ctl .track i:after{content:"";position:absolute;right:-5px;top:-3px;width:10px;height:10px;border-radius:50%;background:#fff}
.bar-ctl time{font-size:11px}
.tabs{display:flex;gap:22px;padding:0 4px;margin:14px 0 10px;font-size:13px;color:var(--muted);border-bottom:1px solid var(--line)}
.tabs a{padding-bottom:10px;text-decoration:none;color:inherit}
.tabs a.on{color:var(--brand);font-weight:600;border-bottom:2px solid var(--brand)}
.note{display:flex;gap:10px;padding:11px 0;border-bottom:1px dashed var(--line);font-size:12.5px;color:var(--ink-2)}
.note .tm{font-size:11px;color:var(--brand);background:var(--brand-soft);border-radius:5px;padding:2px 6px;height:fit-content}
.note .who{margin-left:auto;font-size:11px;color:var(--muted-2)}
.kaodian{padding:14px}
.kaodian h4{font-size:13px;margin-bottom:12px}
.point{display:flex;gap:9px;align-items:flex-start;padding:9px 0;font-size:12px;color:var(--ink-2);border-bottom:1px dashed var(--line)}
.point i{width:16px;height:16px;border-radius:50%;border:1.5px solid #cbd5e1;flex:none;margin-top:1px}
.point.ok i{background:var(--ok);border-color:var(--ok);position:relative}
.point.ok i:after{content:"";position:absolute;left:4px;top:2px;width:5px;height:8px;border:solid #fff;
  border-width:0 1.5px 1.5px 0;transform:rotate(45deg)}
.mastery{margin-top:14px;padding:12px;border-radius:12px;background:#f8fafc;border:1px solid var(--line)}
.mastery .row{display:flex;justify-content:space-between;font-size:11.5px;color:var(--muted);margin:8px 0 5px}
/* ---------- exam ---------- */
.exambar{height:56px;background:#fff;border-bottom:1px solid var(--line);display:flex;align-items:center;
  padding:0 26px;gap:14px}
.exambar b{font-size:14px}
.timer{margin-left:auto;display:flex;align-items:center;gap:8px;font-size:13px;color:#b91c1c;
  background:#fef2f2;border:1px solid #fecaca;border-radius:9px;padding:7px 12px;font-weight:600}
.exam{display:grid;grid-template-columns:1fr 320px;gap:18px;margin-top:20px}
.qhead{display:flex;align-items:center;gap:10px;font-size:12px;color:var(--muted);margin-bottom:12px}
.qtext{font-size:16px;line-height:1.75;color:var(--ink);margin-bottom:20px}
.opt{display:flex;gap:12px;align-items:flex-start;padding:14px 16px;border:1px solid var(--line);border-radius:12px;
  margin-bottom:11px;background:#fff;font-size:13.5px;color:var(--ink-2)}
.opt .k{width:24px;height:24px;border-radius:50%;border:1px solid #cbd5e1;display:flex;align-items:center;
  justify-content:center;font-size:12px;flex:none;color:var(--muted)}
.opt.on{border-color:#93c5fd;background:#f5f9ff;box-shadow:0 0 0 3px rgba(37,99,235,.08)}
.opt.on .k{background:var(--brand);border-color:var(--brand);color:#fff}
.examfoot{display:flex;align-items:center;gap:10px;margin-top:18px}
.sheet{padding:16px}
.sheet .g{display:grid;grid-template-columns:repeat(6,1fr);gap:8px;margin-top:12px}
.cell{height:32px;border-radius:8px;border:1px solid var(--line);display:flex;align-items:center;
  justify-content:center;font-size:11.5px;color:var(--muted);background:#fff}
.cell.done{background:var(--brand-soft);border-color:#c7d7fe;color:var(--brand);font-weight:600}
.cell.cur{background:var(--brand);border-color:var(--brand);color:#fff;font-weight:700}
.cell.flag{background:var(--gold-soft);border-color:#fde68a;color:var(--gold)}
.legend{display:flex;gap:12px;font-size:11px;color:var(--muted);margin-top:14px;flex-wrap:wrap}
.legend i{width:9px;height:9px;border-radius:3px;display:inline-block;margin-right:5px}
/* ---------- cert page ---------- */
.steps{display:flex;gap:12px;margin:6px 0 18px}
.step{flex:1;padding:14px 16px;border-radius:13px;background:#fff;border:1px solid var(--line);display:flex;gap:11px;align-items:center}
.step .num{width:26px;height:26px;border-radius:50%;display:flex;align-items:center;justify-content:center;
  background:var(--ok);color:#fff;font-size:12px;flex:none}
.step h4{font-size:12.5px}
.step p{font-size:11px;color:var(--muted);margin-top:2px}
.certgrid{display:grid;grid-template-columns:1fr 340px;gap:18px;align-items:start}
.cert{position:relative;border-radius:18px;overflow:hidden;border:1px solid #f1d9a8;
  background:linear-gradient(135deg,#fffdf7,#fff8e8 55%,#fef3c7);box-shadow:0 24px 50px -30px rgba(180,83,9,.45)}
.cert .top{padding:22px 26px;display:flex;align-items:center;gap:12px;
  border-bottom:1px dashed #e7cf9b}
.cert .seal{width:42px;height:42px;border-radius:12px;background:linear-gradient(135deg,#b45309,#f59e0b);
  color:#fff;display:flex;align-items:center;justify-content:center;box-shadow:0 10px 20px -12px rgba(180,83,9,.7)}
.cert .top h3{font-size:16px;color:#7c2d12}
.cert .top p{font-size:11.5px;color:#a16207;margin-top:2px}
.cert .body{padding:22px 26px;display:grid;grid-template-columns:1fr 120px;gap:18px}
.cert .name{font-size:26px;letter-spacing:6px;color:#7c2d12;margin:4px 0 4px}
.cert .kv{display:grid;grid-template-columns:78px 1fr;gap:6px 0;font-size:12px;color:#92400e;margin-top:12px}
.cert .kv b{color:#7c2d12;font-weight:600}
.qr{width:120px;height:120px;background:#fff;border:1px solid #f1d9a8;border-radius:10px;padding:8px}
.cert .foot{padding:12px 26px;background:rgba(255,255,255,.6);border-top:1px dashed #e7cf9b;
  display:flex;gap:10px;align-items:center;font-size:11px;color:#a16207}
.verify{padding:16px}
.verify h4{font-size:13px;margin-bottom:10px}
.verify .input{height:38px;border:1px solid var(--line);border-radius:9px;display:flex;align-items:center;
  padding:0 12px;font-size:12px;color:var(--muted-2);background:#f8fafc;margin-bottom:10px}
.verify .ok{display:flex;gap:8px;align-items:center;font-size:11.5px;color:var(--ok);background:var(--ok-soft);
  border:1px solid #bbf7d0;border-radius:9px;padding:9px 11px}
.notice{margin-top:12px;font-size:11.5px;color:var(--warn);background:var(--gold-soft);border:1px solid #fde68a;
  border-radius:9px;padding:10px 11px;line-height:1.6}
"""

NAV = """
<div class="nav"><div class="wrap">
  <div class="logo"><div class="mark">职</div><b>职考云</b><span class="beta">BETA</span></div>
  <div class="menu">
    <a href="#" class="on">首页</a><a href="#">证书方向</a><a href="#">题库</a><a href="#">我的学习</a>
  </div>
  <div class="search">🔍 搜索证书、课包、考点</div>
  <div class="iconbtn">🔔</div>
  <div class="avatar">林</div>
</div></div>
"""


def qr_svg(size=21, seed=7):
    random.seed(seed)
    cells = []
    for y in range(size):
        for x in range(size):
            finder = (x < 7 and y < 7) or (x > size - 8 and y < 7) or (x < 7 and y > size - 8)
            on = finder or random.random() < 0.44
            if finder:
                border = x in (0, 6) or y in (0, 6) or (x in (2, 3, 4) and y in (2, 3, 4))
                on = border
                if x > size - 8:
                    lx, ly = x - (size - 7), y
                    on = lx in (0, 6) or ly in (0, 6) or (lx in (2, 3, 4) and ly in (2, 3, 4))
                if y > size - 8:
                    lx, ly = x, y - (size - 7)
                    on = lx in (0, 6) or ly in (0, 6) or (lx in (2, 3, 4) and ly in (2, 3, 4))
            if on:
                cells.append('<rect x="%d" y="%d" width="1" height="1"/>' % (x, y))
    return ('<svg width="102" height="102" viewBox="0 0 21 21" fill="#0f172a" shape-rendering="crispEdges">'
            + "".join(cells) + "</svg>")


HOME = """
<div class="wrap">
  <div class="hero">
    <div class="copy">
      <div class="eyebrow">2026 上半年 · 初级会计职称 · 报名倒计时 23 天</div>
      <h1>学 · 练 · 考 · 证，一条闭环拿到第一张职业证书</h1>
      <p>课包 + 题库 + 模拟考 + 证书签发与验真，全流程在线完成</p>
      <div class="actions">
        <button class="btn primary">免费试听 3 节</button>
        <button class="btn line">查看考证方向</button>
      </div>
    </div>
    <div class="stats">
      <div class="stat"><b>12.8w</b><span>累计学员</span></div>
      <div class="stat"><b>87.6%</b><span>首考通过率</span></div>
      <div class="stat"><b>3.2w</b><span>已签发证书</span></div>
    </div>
  </div>

  <div class="sec-title" style="margin-top:24px"><h2>考证方向</h2><span>按证书选课，路径自动生成</span><span class="more">全部 12 个方向 ›</span></div>
  <div class="dirs">
    <div class="dir"><div class="ic">📘</div><div><h3>初级会计职称</h3><p>财政部统考 · 每年 5 月</p>
      <div class="foot"><span class="badge blue">12 个课包</span><span>报名 1 月</span></div></div></div>
    <div class="dir"><div class="ic gn">🎓</div><div><h3>教师资格证</h3><p>中小学 · 上半年 3 月笔试</p>
      <div class="foot"><span class="badge green">9 个课包</span><span>报名 1 月</span></div></div></div>
    <div class="dir"><div class="ic g">🖥️</div><div><h3>软考中级</h3><p>系统集成项目管理工程师</p>
      <div class="foot"><span class="badge gold">7 个课包</span><span>报名 3 月</span></div></div></div>
    <div class="dir"><div class="ic">👥</div><div><h3>企业人力资源管理师</h3><p>四级 / 三级 · 全年滚动</p>
      <div class="foot"><span class="badge blue">6 个课包</span><span>随报随考</span></div></div></div>
  </div>

  <div class="sec-title" style="margin-top:26px"><h2>推荐课包</h2><span>根据你的学习记录推荐</span><span class="more">查看全部 ›</span></div>
  <div class="courses">
    <div class="course">
      <div class="cover c1"><span class="tag">2026 新版</span><span class="t">初级会计 · 零基础全程班</span></div>
      <div class="cbody">
        <h3>《初级会计实务》+《经济法基础》双科联报</h3>
        <div class="meta"><span>👩‍🏫 王敏</span><span>86 学时</span><span>1.2w 人在学</span></div>
        <div class="prog"><span>已学 62%</span><div class="bar"><i style="width:62%"></i></div><span>53/86</span></div>
        <div class="price"><b>¥699</b><s>¥999</s><span class="go">继续学习 ›</span></div>
      </div>
    </div>
    <div class="course">
      <div class="cover c2"><span class="tag">科目二</span><span class="t">教师资格 · 教育知识与能力</span></div>
      <div class="cbody">
        <h3>科目二系统精讲 + 主观题模板 + 3 套模考</h3>
        <div class="meta"><span>👨‍🏫 李昊</span><span>54 学时</span><span>8,421 人在学</span></div>
        <div class="prog"><span>已学 18%</span><div class="bar"><i style="width:18%"></i></div><span>10/54</span></div>
        <div class="price"><b>¥499</b><s>¥699</s><span class="go">继续学习 ›</span></div>
      </div>
    </div>
    <div class="course">
      <div class="cover c3"><span class="tag">软考中级</span><span class="t">系统集成 · 案例分析专项</span></div>
      <div class="cbody">
        <h3>计算题专项 + 真题精讲 + 案例模板库</h3>
        <div class="meta"><span>👨‍🏫 张锐</span><span>72 学时</span><span>5,930 人在学</span></div>
        <div class="prog"><span>未开始</span><div class="bar"><i style="width:0%"></i></div><span>0/72</span></div>
        <div class="price"><b>¥899</b><s>¥1,199</s><span class="go">立即报名 ›</span></div>
      </div>
    </div>
  </div>

  <div class="sec-title" style="margin-top:26px"><h2>考证闭环</h2><span>从选课到拿证的四个环节，系统自动衔接</span></div>
  <div class="flow">
    <div class="fstep"><span class="n">1</span><div><h4>选方向 · 配课包</h4><p>按证书匹配课程与题库</p></div></div>
    <div class="fstep"><span class="n">2</span><div><h4>学 · 看课记考点</h4><p>学时自动累计</p></div></div>
    <div class="fstep"><span class="n">3</span><div><h4>练 · 错题按考点聚合</h4><p>薄弱点定向重练</p></div></div>
    <div class="fstep"><span class="n">4</span><div><h4>考 · 发证 · 验真</h4><p>学时+成绩双达标才签发</p></div></div>
  </div>
</div>
"""

LEARN = """
<div class="wrap">
  <div class="crumb">我的学习 <span>/</span> 初级会计 · 零基础全程班 <span>/</span> <b>第 3 章 资产</b></div>
  <div class="learn">
    <div class="card side">
      <h4>课程目录</h4>
      <div class="chap"><b>第 1 章 会计概述</b><span class="n">6/6</span></div>
      <div class="chap"><b>第 2 章 会计基础</b><span class="n">8/8</span></div>
      <div class="chap"><b>第 3 章 资产</b><span class="n">14/22</span></div>
      <div class="lesson done"><span class="dot"></span>3.1 货币资金<span class="du">18:20</span></div>
      <div class="lesson on"><span class="dot"></span>3.2 存货的初始计量<span class="du">24:05</span></div>
      <div class="lesson"><span class="dot"></span>3.3 存货的后续计量<span class="du">21:40</span></div>
      <div class="lesson"><span class="dot"></span>3.4 存货跌价准备<span class="du">26:12</span></div>
      <div class="lesson"><span class="dot"></span>3.5 固定资产取得<span class="du">30:08</span></div>
      <div class="chap" style="margin-top:6px"><b>第 4 章 负债</b><span class="n">0/16</span></div>
      <div class="chap"><b>第 5 章 所有者权益</b><span class="n">0/9</span></div>
      <div class="chap"><b>第 6 章 收入费用利润</b><span class="n">0/18</span></div>
    </div>

    <div>
      <div class="player">
        <div class="stage">
          <div class="ttl">3.2 存货的初始计量<small>王敏 · 初级会计实务 · 第 3 章</small></div>
          <div class="play"><i></i></div>
        </div>
        <div class="bar-ctl">
          <time>10:05</time><div class="track"><i></i></div><time>24:05</time>
          <time>1.0x</time><time>高清</time>
        </div>
      </div>
      <div class="tabs"><a href="#" class="on">课件</a><a href="#">笔记 12</a><a href="#">答疑 3</a><a href="#">相关题目</a></div>
      <div class="card" style="padding:4px 16px">
        <div class="note"><span class="tm">08:12</span><span>外购存货成本 = 买价 + 相关税费 + 运输费 + 装卸费 + 合理损耗</span><span class="who">我 · 3 天前</span></div>
        <div class="note"><span class="tm">13:40</span><span>注意：合理损耗计入成本，不计入营业外支出，这是易错点</span><span class="who">我 · 3 天前</span></div>
        <div class="note"><span class="tm">18:26</span><span>老师这里的例题和 2025 年真题第 5 题很像，注意区分</span><span class="who">李同学 · 昨天</span></div>
      </div>
    </div>

    <div class="card kaodian">
      <h4>本节考点 · 掌握度</h4>
      <div class="point ok"><i></i><span>存货采购成本的构成</span></div>
      <div class="point ok"><i></i><span>合理损耗与不正常损耗的区分</span></div>
      <div class="point"><i></i><span>委托加工物资的成本计算</span></div>
      <div class="mastery">
        <div class="row"><span>本节掌握度</span><b style="color:#059669">72%</b></div>
        <div class="bar"><i style="width:72%"></i></div>
        <div class="row"><span>错题数</span><span>3 题</span></div>
        <div class="row"><span>建议练习</span><span>8 题</span></div>
        <button class="btn primary" style="width:100%;margin-top:10px;padding:9px">去做本节练习</button>
        <button class="btn ghost" style="width:100%;margin-top:8px;padding:9px">下一节 3.3</button>
      </div>
    </div>
  </div>

  <div class="stats-strip">
    <div class="mstat"><b>42<em>分钟</em></b><span>今日学习时长</span></div>
    <div class="mstat"><b>12<em>天</em></b><span>连续打卡</span></div>
    <div class="mstat"><b>8<em>/ 1,204</em></b><span>本周班级排名</span></div>
    <div class="mstat"><b>3<em>题</em></b><span>待复习错题</span></div>
  </div>
</div>
"""

EXAM = """
<div class="exambar">
  <b>初级会计 · 模拟考（第 2 套）</b>
  <span class="badge blue">正式考题型</span>
  <span class="tiny muted">共 30 题 · 满分 100 · 60 分及格</span>
  <div class="timer">⏱ 剩余 44:12</div>
  <button class="btn primary" style="padding:9px 22px">交卷</button>
</div>
<div class="wrap">
  <div class="exam">
    <div>
      <div class="qhead"><span class="badge blue">第 12 / 30 题</span><span class="badge green">单选题</span><span>1 分</span><span style="margin-left:auto">标记本题</span></div>
      <div class="qtext">下列关于存货跌价准备转回的表述中，正确的是（　　）。</div>
      <div class="opt on"><span class="k">A</span><span>转回金额应以原已计提的存货跌价准备金额为限，转回后账面价值不超过历史成本</span></div>
      <div class="opt"><span class="k">B</span><span>只要存货可变现净值回升，就应按回升金额全额转回</span></div>
      <div class="opt"><span class="k">C</span><span>转回的存货跌价准备应计入资本公积</span></div>
      <div class="opt"><span class="k">D</span><span>存货跌价准备一经计提，不得转回</span></div>
      <div class="examfoot">
        <button class="btn ghost">上一题</button>
        <button class="btn primary">下一题</button>
        <span class="tiny muted" style="margin-left:12px">已答 11 题 · 剩余 19 题</span>
      </div>
    </div>
    <div class="card sheet">
      <div style="display:flex;align-items:center"><b style="font-size:13px">答题卡</b><span class="tiny muted" style="margin-left:auto">11 / 30</span></div>
      <div class="g">
        <div class="cell done">1</div><div class="cell done">2</div><div class="cell done">3</div><div class="cell done">4</div><div class="cell done">5</div><div class="cell done">6</div>
        <div class="cell done">7</div><div class="cell done">8</div><div class="cell flag">9</div><div class="cell done">10</div><div class="cell done">11</div><div class="cell cur">12</div>
        <div class="cell">13</div><div class="cell">14</div><div class="cell">15</div><div class="cell">16</div><div class="cell">17</div><div class="cell">18</div>
        <div class="cell">19</div><div class="cell">20</div><div class="cell">21</div><div class="cell">22</div><div class="cell">23</div><div class="cell">24</div>
        <div class="cell">25</div><div class="cell">26</div><div class="cell">27</div><div class="cell">28</div><div class="cell">29</div><div class="cell">30</div>
      </div>
      <div class="legend">
        <span><i style="background:#2563eb"></i>当前</span>
        <span><i style="background:#eef2ff;border:1px solid #c7d7fe"></i>已答</span>
        <span><i style="background:#fffbeb;border:1px solid #fde68a"></i>标记</span>
        <span><i style="background:#fff;border:1px solid #e6ebf4"></i>未答</span>
      </div>
      <div class="notice">交卷后立即出分；成绩 ≥ 60 且学时达标，将自动进入证书签发队列。</div>
    </div>
  </div>

  <div class="pointrow">
    <div class="card" style="padding:16px">
      <b style="font-size:13px">本题考点</b>
      <div class="tagline">
        <span class="chip">存货跌价准备</span><span class="chip">可变现净值</span>
        <span class="chip">账面价值与历史成本</span><span class="chip">资产减值损失</span>
      </div>
      <div class="tiny muted" style="margin-top:12px;line-height:1.8">
        近 3 年真题中该考点出现 <b style="color:#0f172a">11 次</b>，你的历史正确率 <b style="color:#d97706">64%</b>，
        建议交卷后进入「错题本 · 按考点重练」巩固。
      </div>
    </div>
    <div class="card" style="padding:16px">
      <b style="font-size:13px">答题提示</b>
      <div class="tiny muted" style="margin-top:10px;line-height:1.9">
        · 交卷前可随时标记题目<br>
        · 断网自动本地暂存，恢复后同步<br>
        · 剩余 10 分钟将提示一次
      </div>
    </div>
  </div>
</div>
"""

CERT = """
<div class="wrap">
  <div class="crumb" style="margin-top:20px">我的学习 <span>/</span> <b>我的证书</b></div>
  <div class="steps">
    <div class="step"><span class="num">✓</span><div><h4>学时达标</h4><p>120 / 120 学时</p></div></div>
    <div class="step"><span class="num">✓</span><div><h4>模拟考通过</h4><p>第 2 套 · 86 分</p></div></div>
    <div class="step"><span class="num">✓</span><div><h4>正式考合格</h4><p>2026-05-16 · 82 分</p></div></div>
    <div class="step" style="border-color:#a7f3d0;background:#f0fdf4"><span class="num" style="background:#059669">🎉</span><div><h4>证书已签发</h4><p>2026-05-20 · 可下载</p></div></div>
  </div>

  <div class="certgrid">
    <div class="cert">
      <div class="top">
        <div class="seal">🏅</div>
        <div><h3>职业资格证书 · 初级会计职称</h3><p>职考云职业技能认证中心 · 编号规则 ZK-YYYY-NNNNNN</p></div>
        <span class="badge gold" style="margin-left:auto">有效</span>
      </div>
      <div class="body">
        <div>
          <div class="tiny" style="color:#a16207">持证人</div>
          <div class="name">林 晓</div>
          <div class="kv">
            <span>证书编号</span><b>ZK-2026-000137</b>
            <span>发证日期</span><b>2026-05-20</b>
            <span>有效期至</span><b>2031-05-19</b>
            <span>考核成绩</span><b>82 / 100</b>
          </div>
        </div>
        <div>
          <div class="qr">__QR__</div>
          <div class="tiny" style="color:#a16207;text-align:center;margin-top:6px">扫码验真</div>
        </div>
      </div>
      <div class="foot">🔒 证书编号全局唯一 · 验真接口公开可查 · 到期前 30 天自动提醒复证</div>
    </div>

    <div>
      <div class="card verify">
        <h4>证书验真</h4>
        <div class="input">输入证书编号 / 姓名</div>
        <div class="ok">✓ ZK-2026-000137 真实有效 · 2026-05-20 签发</div>
        <div class="tiny muted" style="margin-top:10px;line-height:1.7">验真接口无需登录：<br>GET /api/v1/cert/verify/{verifyCode}</div>
      </div>
      <div class="card" style="margin-top:14px;padding:16px">
        <h4 style="font-size:13px">到期复证</h4>
        <div class="tiny muted" style="margin-top:6px">距有效期届满 <b style="color:#b45309">1,824</b> 天</div>
        <div class="bar" style="margin:10px 0"><i style="width:18%"></i></div>
        <div class="notice" style="margin-top:4px">到期前 30 天将通过站内信 + 短信提醒，可直接续期课包继续学习。</div>
        <button class="btn ghost" style="width:100%;margin-top:12px">下载 PDF 证书</button>
      </div>
    </div>
  </div>

  <div class="sec-title" style="margin-top:22px"><h2>继续考取</h2><span>已拿证后可衔接的下一级证书</span></div>
  <div class="next">
    <div class="nc"><div class="ic">📗</div><div><h4>中级会计职称</h4><p>需 2 年相关工作经验</p></div><span class="go">了解 ›</span></div>
    <div class="nc"><div class="ic">📊</div><div><h4>注册会计师（CPA）</h4><p>6 科 · 5 年通过</p></div><span class="go">了解 ›</span></div>
    <div class="nc"><div class="ic">🎯</div><div><h4>税务师</h4><p>5 科 · 3 年通过</p></div><span class="go">了解 ›</span></div>
  </div>
</div>
"""

SHELL = """<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8"><title>__TITLE__</title>
<style>__CSS__</style></head>
<body><div class="page">__NAV__ __BODY__</div></body></html>
"""

PAGES = [
    ("page-1-home.html", "职考云 · 首页", HOME),
    ("page-2-learn.html", "职考云 · 学习页", LEARN),
    ("page-3-exam.html", "职考云 · 考试答题", EXAM),
    ("page-4-cert.html", "职考云 · 我的证书", CERT.replace("__QR__", qr_svg())),
]

for name, title, body in PAGES:
    nav = "" if name.startswith("page-3") else NAV
    html = SHELL.replace("__TITLE__", title).replace("__CSS__", CSS).replace("__NAV__", nav).replace("__BODY__", body)
    with io.open(os.path.join(OUT, name), "w", encoding="utf-8", newline="\n") as f:
        f.write(html)
    print("wrote", name, len(html), "bytes")
