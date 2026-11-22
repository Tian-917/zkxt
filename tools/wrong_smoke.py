# -*- coding: utf-8 -*-
"""错题本冒烟：练习卷故意全选 A -> 错题入库 -> 按考点聚合 -> 重练答对 -> 自动掌握"""
import json
import subprocess
import time
import urllib.request

EXAM = "http://localhost:8089"
HEAD = {"Content-Type": "application/json", "user-info": "1"}
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"


def sql(q, db="zk_exam"):
    r = subprocess.run([MYSQL, "-h192.168.150.101", "-uroot", "-p123", "-D", db,
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", q],
                       capture_output=True)
    return [l.split("\t") for l in r.stdout.decode("utf-8", "ignore").splitlines() if l]


def call(url, body=None, method=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    m = method or ("POST" if data else "GET")
    req = urllib.request.Request(url, data=data, method=m)
    for k, v in HEAD.items():
        req.add_header(k, v)
    with urllib.request.urlopen(req, timeout=60) as r:
        raw = r.read().decode("utf-8")
        return json.loads(raw) if raw else None


# 1. 练习卷（type=1）
exam = call(EXAM + "/exams", {"courseId": 1, "sectionId": 3, "type": 1})
exam_id, questions = exam["id"], exam["questions"]
print("练习卷: id=%s 题数=%d" % (exam_id, len(questions)))

# 2. 全部选 A —— 故意做错大部分
wrong_submit = {"id": exam_id,
                "examDetails": [{"questionId": str(q["id"]), "answer": "A"} for q in questions]}
detail = call(EXAM + "/exams/details", wrong_submit)
print("交卷: 答对 %d/%d" % (sum(1 for d in detail if d["correct"]), len(detail)))
time.sleep(1)

# 3. 错题本
lst = call(EXAM + "/wrong/list?mastered=0")
print("未掌握错题数: %d" % len(lst))
if lst:
    print("  首题: %s | 我的答案=%s | 正确答案=%s" % (lst[0]["name"][:24], lst[0]["lastAnswer"], lst[0]["answer"]))

points = call(EXAM + "/wrong/points")
print("按考点聚合: %d 个考点" % len(points))
for p in points[:3]:
    print("  %s | 错题 %s 道 / 累计错 %s 次" % (p["sectionName"], p["questionCount"], p["wrongTimes"]))

# 4. 重练同一小节 —— 这次全答对，历史错题应自动变成已掌握
exam2 = call(EXAM + "/exams", {"courseId": 1, "sectionId": 3, "type": 1})
answers = {r[0]: r[1] for r in sql(
    "SELECT d.id, d.answer FROM question_detail d WHERE d.id IN (%s)"
    % ",".join(str(q["id"]) for q in exam2["questions"]))}
right_submit = {"id": exam2["id"],
                "examDetails": [{"questionId": str(q["id"]), "answer": answers.get(str(q["id"]), "A")}
                                for q in exam2["questions"]]}
d2 = call(EXAM + "/exams/details", right_submit)
print("重练交卷: 答对 %d/%d" % (sum(1 for d in d2 if d["correct"]), len(d2)))
time.sleep(1)

left = call(EXAM + "/wrong/list?mastered=0")
done = call(EXAM + "/wrong/list?mastered=1")
print("重练后：未掌握 %d 题，已掌握 %d 题" % (len(left), len(done)))
