# -*- coding: utf-8 -*-
"""P1 第三批：让"学时达标"这条规则能真跑通。

两件事：
1. 给课包的小节时长做一次放大（原数据每节 4 分钟太假，考证课一节 20~40 分钟才合理）
2. 给 user_id=1 在课程 1 下补 6 条已完成的学习记录

幂等：时长只在「最大值 < 1000 秒」时才放大，重复执行不会越乘越大。
"""
import io
import os
import subprocess
import sys

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
HOST, USER, PWD = "192.168.150.101", "root", "123"
ROOT = r"D:\Java\tianji"
OUT = os.path.join(ROOT, "docs", "sql", "seed-study-records.sql")
SCALE = 8


def q(sql, db="zk_course"):
    r = subprocess.run([MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD, "-D", db,
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", sql],
                       capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "ignore"))
    return [l.split("\t") for l in r.stdout.decode("utf-8", "ignore").splitlines() if l]


lines = ["-- 职考云 · 学时相关种子（由 tools/seed/gen_study_records.py 生成）", ""]

# 按课程判断是否需要放大：课程内小节最大时长 < 900 秒 => 还没放大过
need = q("SELECT COUNT(*) FROM (SELECT course_id, MAX(media_duration) m FROM course_catalogue "
         "WHERE type=2 GROUP BY course_id HAVING m < 900) t")[0][0]
if int(need) > 0:
    lines.append("-- 1. 小节时长放大 %d 倍（每节从 4 分钟变成 30 分钟量级），按课程判断是否已放大" % SCALE)
    lines.append(
        "UPDATE `zk_course`.`course_catalogue` c "
        "JOIN (SELECT course_id, MAX(media_duration) m FROM `zk_course`.`course_catalogue` "
        "      WHERE type = 2 GROUP BY course_id) t ON t.course_id = c.course_id "
        "SET c.media_duration = c.media_duration * %d "
        "WHERE c.type = 2 AND c.media_duration > 0 AND t.m < 900;" % SCALE)
    print("时长将放大 %d 倍（%s 个课程需要放大）" % (SCALE, need))
else:
    lines.append("-- 1. 时长均已放大，跳过")
    print("时长已是放大后的值，跳过")
lines.append("")

# 课程 1 的前 6 个小节
sections = q("SELECT id, media_duration FROM course_catalogue WHERE course_id=1 AND type=2 "
             "ORDER BY c_index LIMIT 6")
lesson = q("SELECT id FROM learning_lesson WHERE user_id=1 AND course_id=1", "zk_learning")
lesson_id = lesson[0][0] if lesson else "0"

lines.append("-- 2. user_id=1 在课程1 的 6 个已完成小节")
lines.append("DELETE FROM `zk_learning`.`learning_record` WHERE user_id = 1;")
total = 0
for idx, (sid, dur) in enumerate(sections):
    d = int(dur) * (SCALE if int(need) > 0 else 1)
    total += d
    lines.append(
        "INSERT INTO `zk_learning`.`learning_record` "
        "(id, lesson_id, section_id, user_id, moment, finished, create_time, finish_time) "
        "VALUES (%d, %s, %s, 1, %d, b'1', NOW(), NOW());"
        % (1900000000000000400 + idx, lesson_id, sid, d))
lines.append("")

print("已完成小节数：%d，累计时长 %d 秒 ≈ %.1f 分钟" % (len(sections), total, total / 60.0))
lines.append("-- 累计学时约 %d 分钟（证书域要求 120）" % (total // 60))

# 有些小节原始数据没有媒资（media_duration=0），前端模拟播放无法推进，统一补一个合理时长
zero = q("SELECT COUNT(*) FROM course_catalogue WHERE type=2 AND (media_duration IS NULL OR media_duration = 0)")[0][0]
if int(zero) > 0:
    lines.append("-- 补全缺失的小节时长（%s 条，25~43 分钟错开），否则前端模拟播放无从推进" % zero)
    lines.append(
        "UPDATE `zk_course`.`course_catalogue` SET media_duration = 1500 + MOD(c_index, 10) * 120 "
        "WHERE type = 2 AND (media_duration IS NULL OR media_duration = 0);")
    print("将补全 %s 条缺失时长" % zero)

sql = "\n".join(lines)
with io.open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write(sql)
print("SQL 已生成：%s" % OUT)

if "--apply" in sys.argv:
    r = subprocess.run([MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD, "--default-character-set=utf8mb4"],
                       stdin=io.open(OUT, "rb"), capture_output=True)
    print("执行 exit=%d" % r.returncode)
    if r.stderr:
        print(r.stderr.decode("utf-8", "ignore")[:500])
