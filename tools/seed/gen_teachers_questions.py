# -*- coding: utf-8 -*-
"""P1 第二批种子：4 位讲师 + 30 道考证题。

- 讲师：zk_user.user(type=3) + zk_user.user_detail(type=3)，再重建 course_teacher 关联
- 题目：zk_exam.question + question_detail + question_biz（挂到各自课程的第一节）

用法：
    python tools/seed/gen_teachers_questions.py            # 只生成 SQL
    python tools/seed/gen_teachers_questions.py --apply    # 生成并执行
"""
import io
import json
import os
import subprocess
import sys

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
HOST, USER, PWD = "192.168.150.101", "root", "123"
ROOT = r"D:\Java\tianji"
OUT = os.path.join(ROOT, "docs", "sql", "seed-teachers-questions.sql")

# admin 的密码哈希（123456），复用它省得生成
PWD_HASH = "$2a$10$4duXTHlB9/jgnaNylGD2rOfwvko/T7fCjqLqpVoqVyRo11GGatyFS"


def q(sql):
    r = subprocess.run([MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD,
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", sql],
                       capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "ignore"))
    return [l.split("\t") for l in r.stdout.decode("utf-8", "ignore").splitlines() if l]


def esc(s):
    return s.replace("\\", "\\\\").replace("'", "''")


# ---------------- 讲师 ----------------
# (id, 登录名, 手机号, 姓名, 性别, 头衔, 简介, 负责的二级分类)
TEACHERS = [
    (1900000000000000001, "wangmin", "13800000001", "王敏", 1, "财会教研组 · 高级讲师",
     "注册会计师，10 年财会考试培训经验，主讲初级/中级会计实务", 2028),
    (1900000000000000002, "lihao", "13800000002", "李昊", 0, "教资教研组 · 首席讲师",
     "教育学硕士，专注教师资格证笔试与面试 8 年", 2027),
    (1900000000000000003, "zhangrui", "13800000003", "张锐", 0, "软考教研组 · 高级讲师",
     "系统架构设计师，PMP，主讲系统集成与项目管理", 2080),
    (1900000000000000004, "chenjing", "13800000004", "陈静", 1, "建工 / 医药教研组 · 讲师",
     "一级建造师，主讲二建法规与施工管理、执业药师", 2029),
]
# 其余方向也挂到这几位老师上
FALLBACK = {2039: 1900000000000000004, 2046: 1900000000000000004,
            2068: 1900000000000000002, 2081: 1900000000000000002,
            2079: 1900000000000000002}
BY_CATE = {t[7]: t[0] for t in TEACHERS}

lines = ["-- 职考云 · 讲师与题库种子（由 tools/seed/gen_teachers_questions.py 生成）", ""]

lines.append("-- 1. 讲师账号")
for tid, uname, phone, name, gender, job, intro, _cate in TEACHERS:
    lines.append(
        "INSERT INTO `zk_user`.`user` (id, username, cell_phone, password, type, status) "
        "VALUES (%d, '%s', '%s', '%s', 3, 1) "
        "ON DUPLICATE KEY UPDATE username=VALUES(username), cell_phone=VALUES(cell_phone), type=3;" %
        (tid, uname, phone, PWD_HASH))
    lines.append(
        "INSERT INTO `zk_user`.`user_detail` (id, type, name, gender, job, intro, role_id, course_amount, dep_id) "
        "VALUES (%d, 3, '%s', %d, '%s', '%s', 0, 0, 0) "
        "ON DUPLICATE KEY UPDATE type=3, name=VALUES(name), job=VALUES(job), intro=VALUES(intro);" %
        (tid, esc(name), gender, esc(job), esc(intro)))
lines.append("")

lines.append("-- 2. 重建课程-讲师关联（原数据引用的 teacher_id 在用户表里不存在）")
lines.append("DELETE FROM `zk_course`.`course_teacher` WHERE teacher_id NOT IN (%s);"
             % ", ".join(str(t[0]) for t in TEACHERS))
courses = q("SELECT id, second_cate_id FROM zk_course.course ORDER BY id")
for idx, (cid, cate2) in enumerate(courses):
    tid = BY_CATE.get(int(cate2), FALLBACK.get(int(cate2), TEACHERS[0][0]))
    lines.append(
        "REPLACE INTO `zk_course`.`course_teacher` "
        "(id, course_id, teacher_id, is_show, c_index, dep_id, create_time, update_time, creater, updater, deleted) "
        "VALUES (%d, %s, %d, 1, 1, 0, NOW(), NOW(), 1, 1, 0);" %
        (1900000000000000200 + idx, cid, tid))
lines.append("")

# ---------------- 题目 ----------------
# (方向分类, 类型, 题干, [选项], 答案, 解析)
ACCT = (2028, 1)
QUESTIONS = [
    (ACCT, "会计的基本假设不包括下列哪一项？",
     ["货币计量", "持续经营", "会计分期", "权责发生制"], "D",
     "权责发生制是会计基础，不是会计基本假设；四项基本假设为会计主体、持续经营、会计分期、货币计量。"),
    (ACCT, "下列各项中，属于流动资产的是（　）。",
     ["固定资产", "存货", "无形资产", "长期股权投资"], "B",
     "存货预计在一年内变现或耗用，属于流动资产。"),
    (ACCT, "下列各项中，应计入营业外收入的是（　）。",
     ["销售商品收入", "政府补助", "提供劳务收入", "让渡资产使用权收入"], "B",
     "政府补助与日常活动无关，计入营业外收入。"),
    (ACCT, "反映企业某一特定日期财务状况的会计要素是（　）。",
     ["收入", "费用", "所有者权益", "利润"], "C",
     "资产、负债、所有者权益反映财务状况；收入、费用、利润反映经营成果。"),
    (ACCT, "借贷记账法下，资产类账户的增加记入（　）。",
     ["借方", "贷方", "借贷任意一方", "不做记录"], "A",
     "资产、成本、费用类账户增加记借方，减少记贷方。"),
    (ACCT, "下列各项中，不属于会计信息质量要求的是（　）。",
     ["可靠性", "相关性", "可比性", "持续经营"], "D",
     "持续经营是会计基本假设，其余为会计信息质量要求。"),
    ((2028, 2), "下列各项中，属于会计信息质量要求的有（　）。",
     ["可靠性", "相关性", "可比性", "持续经营"], "ABC",
     "持续经营是基本假设；可靠性、相关性、可比性、及时性、重要性、谨慎性、实质重于形式是质量要求。"),
    (ACCT, "存货发出计价方法不包括（　）。",
     ["先进先出法", "月末一次加权平均法", "个别计价法", "双倍余额递减法"], "D",
     "双倍余额递减法是固定资产折旧方法。"),
    (ACCT, "当月增加的固定资产，企业应当（　）。",
     ["当月计提折旧", "次月计提折旧", "当年不计提折旧", "按天折算折旧"], "B",
     "当月增加的固定资产当月不提折旧，从次月起计提。"),
    (ACCT, "下列各项中，不属于无形资产的是（　）。",
     ["专利权", "商标权", "土地使用权", "商誉"], "D",
     "商誉不可辨认，不属于无形资产（企业合并中形成）。"),
    (ACCT, "企业对应收账款计提坏账准备，体现的会计信息质量要求是（　）。",
     ["谨慎性", "重要性", "实质重于形式", "及时性"], "A",
     "不高估资产、不低估负债和费用，体现谨慎性。"),
    (ACCT, "“应付职工薪酬”科目属于（　）。",
     ["资产类", "负债类", "所有者权益类", "损益类"], "B",
     "应付职工薪酬是企业对职工的负债。"),
    (ACCT, "下列各项中，不影响营业利润的是（　）。",
     ["营业收入", "营业成本", "营业外收入", "税金及附加"], "C",
     "营业利润=营业收入-营业成本-税金及附加-期间费用等，不含营业外收入。"),
    (ACCT, "我国增值税一般纳税人销售货物适用的基本税率是（　）。",
     ["9%", "13%", "6%", "3%"], "B",
     "销售货物、提供加工修理修配劳务适用 13% 基本税率。"),
    (ACCT, "我国企业所得税的法定基本税率是（　）。",
     ["15%", "20%", "25%", "33%"], "C",
     "企业所得税基本税率为 25%，小型微利企业适用优惠税率。"),
    (ACCT, "建立劳动关系，应当订立（　）劳动合同。",
     ["口头", "书面", "公证", "电子"], "B",
     "《劳动合同法》要求建立劳动关系应当订立书面劳动合同。"),
    (ACCT, "根据现行规定，企业原始凭证的保管期限一般不少于（　）。",
     ["10 年", "15 年", "30 年", "永久"], "C",
     "会计凭证、账簿保管期限为 30 年，年度财务报告永久保存。"),
    (ACCT, "下列票据中，属于《票据法》调整范围的是（　）。",
     ["发票", "支票", "收据", "提货单"], "B",
     "票据法上的票据包括汇票、本票和支票。"),
    ((2080, 1), "项目管理中，关键路径是指（　）。",
     ["成本最低的路径", "总工期最长的路径", "资源占用最少的路径", "风险最小的路径"], "B",
     "关键路径是网络图中总工期最长的路径，决定项目最短工期。"),
    ((2080, 1), "项目范围管理的首要过程是（　）。",
     ["规划范围管理", "定义范围", "创建 WBS", "确认范围"], "A",
     "范围管理依次为：规划范围管理、收集需求、定义范围、创建 WBS、确认范围、控制范围。"),
    ((2080, 1), "敏捷开发中，负责管理产品待办列表（Product Backlog）的角色是（　）。",
     ["Scrum Master", "产品负责人", "开发团队", "项目经理"], "B",
     "产品负责人（Product Owner）负责产品待办列表的排序与价值最大化。"),
    ((2080, 2), "常用的风险应对策略包括（　）。",
     ["规避", "转移", "减轻", "忽视"], "ABC",
     "风险应对策略包括规避、转移、减轻、接受和上报，“忽视”不是规范策略。"),
    ((2080, 1), "质量管理 PDCA 循环中，“C”代表（　）。",
     ["计划", "执行", "检查", "处理"], "C",
     "PDCA = Plan 计划、Do 执行、Check 检查、Act 处理。"),
    ((2080, 1), "数据库事务 ACID 特性中，“I”代表（　）。",
     ["原子性", "一致性", "隔离性", "持久性"], "C",
     "ACID：原子性 Atomicity、一致性 Consistency、隔离性 Isolation、持久性 Durability。"),
    ((2027, 1), "教师职业道德的核心是（　）。",
     ["爱国守法", "爱岗敬业", "关爱学生", "终身学习"], "C",
     "关爱学生是师德的灵魂，是教师职业道德的核心。"),
    ((2027, 2), "《中小学教师职业道德规范》的内容包括（　）。",
     ["爱国守法", "爱岗敬业", "关爱学生", "以升学率为唯一标准"], "ABC",
     "规范包括爱国守法、爱岗敬业、关爱学生、教书育人、为人师表、终身学习。"),
    ((2027, 1), "新课程改革背景下的教师观强调，教师应当是（　）。",
     ["知识的灌输者", "学生学习的促进者", "课程的执行者", "评价的主导者"], "B",
     "新课改教师观：从知识传授者转变为学生学习的引导者和促进者。"),
    ((2027, 1), "根据皮亚杰的认知发展阶段理论，7~11 岁儿童处于（　）。",
     ["感知运动阶段", "前运算阶段", "具体运算阶段", "形式运算阶段"], "C",
     "7~11 岁为具体运算阶段，具备守恒概念但需具体事物支持。"),
    ((2027, 1), "德育过程的基本矛盾是（　）。",
     ["教育者与受教育者的矛盾", "社会道德要求与学生已有品德水平之间的矛盾",
      "知与行的矛盾", "集体与个人的矛盾"], "B",
     "德育过程的基本矛盾是社会通过教师提出的道德要求与学生已有品德水平之间的矛盾。"),
    ((2027, 1), "我国义务教育的年限是（　）。",
     ["六年", "九年", "十二年", "十五年"], "B",
     "《义务教育法》规定国家实行九年义务教育制度。"),
]

lines.append("-- 3. 题库（30 道，挂到所属课程的第一节）")
section_cache = {}
for cate, cid in sorted({(c[0], c[1]) for c, *_ in QUESTIONS}):
    rows = q("SELECT id FROM zk_course.course WHERE second_cate_id=%d AND status=2 ORDER BY id LIMIT 1" % cate)
    if not rows:
        continue
    course_id = rows[0][0]
    sec = q("SELECT id FROM zk_course.course_catalogue WHERE course_id=%s AND type=2 ORDER BY c_index LIMIT 1" % course_id)
    if sec:
        section_cache[cate] = sec[0][0]

for idx, ((cate, qtype), stem, options, answer, analysis) in enumerate(QUESTIONS):
    qid = 1900000000000000100 + idx + 1
    opts = json.dumps(options, ensure_ascii=False)
    lines.append(
        "INSERT INTO `zk_exam`.`question` (id, name, type, cate_id1, cate_id2, cate_id3, "
        "difficulty, score, answer_times, correct_times, dep_id, creater, updater) "
        "VALUES (%d, '%s', %d, 1004, %d, 0, 2, 5, 0, 0, 0, 1, 1) "
        "ON DUPLICATE KEY UPDATE name=VALUES(name), type=VALUES(type), cate_id2=VALUES(cate_id2);" %
        (qid, esc(stem), qtype, cate))
    lines.append(
        "INSERT INTO `zk_exam`.`question_detail` (id, options, answer, analysis) VALUES (%d, '%s', '%s', '%s') "
        "ON DUPLICATE KEY UPDATE options=VALUES(options), answer=VALUES(answer), analysis=VALUES(analysis);" %
        (qid, esc(opts), esc(answer), esc(analysis)))
    sec_id = section_cache.get(cate)
    if sec_id:
        lines.append(
            "INSERT INTO `zk_exam`.`question_biz` (biz_id, question_id) VALUES (%s, %d) "
            "ON DUPLICATE KEY UPDATE question_id=VALUES(question_id);" % (sec_id, qid))

lines.append("-- 4. 学员报名（考试前置校验要求已报名；顺带让「我的课表」有内容）")
for idx, (cid, _cate2) in enumerate(courses):
    sec = q("SELECT id FROM zk_course.course_catalogue WHERE course_id=%s AND type=2 ORDER BY c_index LIMIT 1" % cid)
    sec_id = sec[0][0] if sec else "NULL"
    lines.append(
        "REPLACE INTO `zk_learning`.`learning_lesson` "
        "(id, user_id, course_id, status, week_freq, plan_status, learned_sections, latest_section_id, create_time, expire_time) "
        "VALUES (%d, 1, %s, 1, 6, 0, 3, %s, NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR));" %
        (1900000000000000300 + idx, cid, sec_id))
lines.append("")

sql = "\n".join(lines)
with io.open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(sql)
print("SQL 已生成：%s（%d 行，%d 道题，%d 位讲师）" % (OUT, len(lines), len(QUESTIONS), len(TEACHERS)))
print("题目所属小节：", section_cache)

if "--apply" in sys.argv:
    r = subprocess.run([MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD, "--default-character-set=utf8mb4"],
                       stdin=io.open(OUT, "rb"), capture_output=True)
    print("执行 exit=%d" % r.returncode)
    if r.stderr:
        print(r.stderr.decode("utf-8", "ignore")[:800])
