# -*- coding: utf-8 -*-
"""端到端冒烟：正式考 -> 全对交卷 -> 考试服务发 MQ -> 证书域自动签发 -> 验真"""
import json
import subprocess
import urllib.request

EXAM = "http://localhost:8089"
CERT = "http://localhost:8091"
HEAD = {"Content-Type": "application/json", "user-info": "1"}
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"


def sql(q):
    r = subprocess.run([MYSQL, "-h192.168.150.101", "-uroot", "-p123", "-D", "zk_exam",
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", q],
                       capture_output=True)
    return [l.split("\t") for l in r.stdout.decode("utf-8", "ignore").splitlines() if l]


def call(url, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method="POST" if data else "GET")
    for k, v in HEAD.items():
        req.add_header(k, v)
    with urllib.request.urlopen(req, timeout=60) as r:
        raw = r.read().decode("utf-8")
        return json.loads(raw) if raw else None


# 1. 创建正式考（type=2），必须先报名 + 没有同小节的正式考记录
exam = call(EXAM + "/exams", {"courseId": 1, "sectionId": 6, "type": 2})
exam_id, questions = exam["id"], exam["questions"]
print("正式考创建: id=%s 题数=%d" % (exam_id, len(questions)))

# 2. 从库里取标准答案，全部答对
ids = ",".join(str(q["id"]) for q in questions)
answers = {r[0]: r[1] for r in sql(
    "SELECT d.id, d.answer FROM question_detail d WHERE d.id IN (%s)" % ids)}
submit = {"id": exam_id,
          "examDetails": [{"questionId": str(q["id"]), "answer": answers.get(str(q["id"]), "A")}
                          for q in questions]}
detail = call(EXAM + "/exams/details", submit)
score = sum(1 for d in detail if d.get("correct")) * 5
print("交卷完成: 答对 %d/%d，得分 %d" % (sum(1 for d in detail if d.get("correct")), len(detail), score))

# 3. 等 MQ 异步消费
import time
for _ in range(10):
    time.sleep(1)
    rows = sql("SELECT COUNT(*) FROM zk_cert.certificate WHERE exam_id=%s" % exam_id)
    if int(rows[0][0]) > 0:
        break

rows = sql("SELECT cert_no, user_name, cert_type, score, verify_code FROM zk_cert.certificate WHERE exam_id=%s" % exam_id)
if rows:
    cert_no, name, ctype, cscore, vcode = rows[0]
    print("证书已签发: %s | %s | %s | %s 分" % (cert_no, name, ctype, cscore))
    verify = call(CERT + "/cert/verify/" + vcode)
    print("验真: valid=%s holder=%s message=%s" % (verify["valid"], verify["holder"], verify["message"]))
else:
    print("!! 未查到证书，检查 exam/cert 日志")
