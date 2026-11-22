# -*- coding: utf-8 -*-
"""P1 种子数据：把课程库改造成"考证平台"的样子。

做三件事：
1. 14 门课 -> 考证课包（名称、分类、价格）
2. 课程目录 -> 考点章节（第N章 xxx / N.M 考点）
3. 题库 -> 会计 / 教资 / 软考 真题风格的选择题

用法：
    python tools/seed/gen_seed.py            # 只生成 SQL
    python tools/seed/gen_seed.py --apply    # 生成并执行
"""
import io
import json
import os
import subprocess
import sys

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
HOST, USER, PWD = "192.168.150.101", "root", "123"
ROOT = r"D:\Java\tianji"
OUT = os.path.join(ROOT, "docs", "sql", "seed-cert-platform.sql")


def q(sql):
    """查库，返回二维数组"""
    r = subprocess.run(
        [MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD, "-D", "zk_course",
         "--default-character-set=utf8mb4",
         "--batch", "--raw", "-N", "-e", sql],
        capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "ignore"))
    text = r.stdout.decode("utf-8", "ignore")
    return [line.split("\t") for line in text.splitlines() if line]


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "''")


# ---------------- 1. 分类 ----------------
NEW_CATES = [
    (2080, "计算机软考", 1004),
    (2081, "人力资源", 1004),
]

# ---------------- 2. 课程 ----------------
# (course_id, 新名称, 二级分类, 价格(分))
COURSES = [
    ("1", "初级会计职称 · 零基础全程班", 2028, 69900),
    ("2", "初级会计 · 经济法基础精讲", 2028, 39900),
    ("3", "中级会计 · 财务管理专项", 2028, 89900),
    ("4", "教师资格 · 综合素质（小学）", 2027, 49900),
    ("5", "教师资格 · 教育知识与能力", 2027, 49900),
    ("6", "软考中级 · 系统集成项目管理工程师", 2080, 89900),
    ("7", "软考中级 · 软件设计师", 2080, 89900),
    ("8", "二级建造师 · 建设工程法规", 2029, 69900),
    ("9", "二级建造师 · 施工管理", 2029, 69900),
    ("10", "执业药师 · 药学专业知识", 2039, 129900),
    ("1549025085494521857", "法考 · 民法精讲", 2046, 159900),
    ("1552558707325374467", "考研英语 · 阅读理解专项", 2068, 59900),
    ("1589888774267072513", "人力资源管理师 · 四级", 2081, 79900),
    ("1589905661084430337", "学历提升 · 自考本科英语", 2079, 39900),
]

# 各方向章节名池
SECTION_TPL = ["考点精讲", "真题解析", "易错点辨析", "高频考点串讲", "章节强化"]
CHAPTERS = {
    2028: ["会计概述", "会计基础", "流动资产", "非流动资产", "负债", "所有者权益", "收入费用利润", "财务报告"],
    2027: ["职业理念", "教育法律法规", "教师职业道德", "文化素养", "基本能力", "教育基础", "学生指导", "班级管理"],
    2080: ["信息化与信息系统", "系统集成技术", "项目管理基础", "立项与范围管理", "进度与成本管理", "质量与风险管理"],
    2029: ["建设工程基本法律知识", "施工许可法律制度", "发承包法律制度", "合同与劳动法律", "施工管理概论", "成本管理"],
    2039: ["药理学基础", "药物分析", "药剂学", "药事管理与法规"],
    2046: ["民法总则", "物权编", "合同编", "人格权编", "婚姻家庭编"],
    2068: ["阅读理解", "新题型", "翻译", "写作"],
    2081: ["人力资源规划", "招聘与配置", "培训与开发", "绩效管理"],
    2079: ["英语（一）", "英语（二）", "语法专项", "写作专项"],
}

lines = []
lines.append("-- 职考云 · 考证平台种子数据（由 tools/seed/gen_seed.py 生成）")
lines.append("USE `zk_course`;")
lines.append("")

# 分类
lines.append("-- 1. 新增二级分类")
for cid, name, parent in NEW_CATES:
    lines.append(
        "INSERT INTO `category` (id, name, parent_id, level, priority, status) "
        "VALUES (%d, '%s', %d, 2, 99, 1) "
        "ON DUPLICATE KEY UPDATE name=VALUES(name);" % (cid, name, parent))
lines.append("")

# 课程
lines.append("-- 2. 课程改造")
for cid, name, cate2, price in COURSES:
    lines.append(
        "UPDATE `course` SET name='%s', first_cate_id=1004, second_cate_id=%d, "
        "third_cate_id=0, price=%d, free=0, status=2 WHERE id=%s;"
        % (esc(name), cate2, price, cid))
lines.append("")

# 目录
lines.append("-- 3. 课程目录 -> 考点章节")
for cid, name, cate2, price in COURSES:
    pool = CHAPTERS.get(cate2, ["考点精讲"])
    rows = q("SELECT id, parent_catalogue_id, type, c_index FROM `course_catalogue` "
             "WHERE course_id=%s ORDER BY c_index, id" % cid)
    chapters = [r for r in rows if r[1] == "0"]
    for i, r in enumerate(chapters):
        chap_id = r[0]
        chap_name = pool[i % len(pool)]
        lines.append(
            "UPDATE `course_catalogue` SET name='第%d章 %s' WHERE id=%s;"
            % (i + 1, esc(chap_name), chap_id))
        children = [c for c in rows if c[1] == chap_id]
        for j, c in enumerate(children):
            if c[2] == "3":
                new_name = "%d.%d 本章练习" % (i + 1, j + 1)
            else:
                tpl = SECTION_TPL[j % len(SECTION_TPL)]
                new_name = "%d.%d %s · %s" % (i + 1, j + 1, chap_name, tpl)
            lines.append("UPDATE `course_catalogue` SET name='%s' WHERE id=%s;"
                         % (esc(new_name), c[0]))
lines.append("")

# ---------------- 4. 题库 ----------------
# zk_exam.question: id,name,type,score,difficulty ; zk_exam.question_detail: id,options,answer,analysis
QUESTIONS = [
    ("1561504460092592130", "下列关于存货跌价准备转回的表述中，正确的是（　）。",
     "A. 转回金额应以原已计提的存货跌价准备金额为限\nB. 只要可变现净值回升，就应全额转回\nC. 转回的跌价准备计入资本公积\nD. 存货跌价准备一经计提不得转回",
     "A", "转回金额以原已计提的跌价准备为限，且转回后账面价值不得超过历史成本。"),
    ("1561505580659625985", "下列各项中，应计入存货采购成本的有（　）。",
     "A. 买价\nB. 运输途中的合理损耗\nC. 入库前的挑选整理费\nD. 采购人员的差旅费",
     "ABC", "采购人员差旅费计入管理费用，不计入存货成本。"),
    ("1561507025660268546", "关于固定资产折旧，下列说法正确的有（　）。",
     "A. 当月增加的固定资产当月计提折旧\nB. 当月减少的固定资产当月仍计提折旧\nC. 已提足折旧仍继续使用的固定资产不再计提\nD. 提前报废的固定资产应补提折旧",
     "BC", "当月增加次月计提；提前报废不补提。"),
    ("1566029032905007000", "教师职业道德的核心是（　）。",
     "A. 爱国守法\nB. 爱岗敬业\nC. 关爱学生\nD. 终身学习",
     "C", "关爱学生是师德的灵魂。"),
    ("1587999779978223618", "系统集成项目管理中，用于表示活动之间逻辑关系的是（　）。",
     "A. 甘特图\nB. 单代号网络图\nC. 资源直方图\nD. 鱼骨图",
     "B", "单代号网络图用节点表示活动、箭线表示逻辑关系。"),
    ("1589901500625817602", "关于项目进度压缩，下列说法正确的是（　）。",
     "A. 赶工通常增加成本\nB. 快速跟进会增加返工风险\nC. 压缩关键路径一定缩短总工期\nD. 资源平衡可以缩短工期",
     "AB", "资源平衡通常延长工期；压缩关键路径需考虑次关键路径。"),
]

lines.append("-- 4. 题库 -> 考证真题风格")
for qid, stem, options, answer, analysis in QUESTIONS:
    # options 是 JSON 列，必须写成 JSON 数组
    opts = json.dumps([o.strip() for o in options.split("\n") if o.strip()], ensure_ascii=False)
    q_type = 2 if len(answer) > 1 else 1   # 1 单选 / 2 多选
    lines.append("UPDATE `zk_exam`.`question` SET name='%s', type=%d, score=5, difficulty=2 "
                 "WHERE id=%s;" % (esc(stem), q_type, qid))
    lines.append("UPDATE `zk_exam`.`question_detail` SET options='%s', answer='%s', analysis='%s' "
                 "WHERE id=%s;" % (esc(opts), esc(answer), esc(analysis), qid))
lines.append("")

sql = "\n".join(lines)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with io.open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(sql)
print("SQL 已生成：%s（%d 行）" % (OUT, len(lines)))

if "--apply" in sys.argv:
    r = subprocess.run(
        [MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD, "--default-character-set=utf8mb4"],
        stdin=io.open(OUT, "rb"), capture_output=True)
    print("执行 exit=%d" % r.returncode)
    if r.stderr:
        print(r.stderr.decode("utf-8", "ignore")[:800])
