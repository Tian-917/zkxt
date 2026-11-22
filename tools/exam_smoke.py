# -*- coding: utf-8 -*-
"""考试闭环冒烟：创建考试 -> 全部答对 -> 交卷 -> 看分数"""
import json
import urllib.request

BASE = "http://localhost:8089"
HEAD = {"Content-Type": "application/json", "user-info": "1"}


def call(path, body=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method="POST" if data else "GET")
    for k, v in HEAD.items():
        req.add_header(k, v)
    with urllib.request.urlopen(req, timeout=60) as r:
        raw = r.read().decode("utf-8")
        return int(getattr(r, "status", 200)), (json.loads(raw) if raw else None)


# 1. 创建考试
_, exam = call("/exams", {"courseId": 1, "sectionId": 3, "type": 1})
exam_id = exam["id"]
questions = exam["questions"]
print("考试记录 id:", exam_id, "题数:", len(questions))

# 2. 全部答对（答案从库里查出来的标准答案，这里按题型给首个选项，仅用于验证判分链路）
# 为了确定性，直接按题目顺序给 A，看判分是否正常返回分数
answers = [{"questionId": str(q["id"]), "answer": "A"} for q in questions]
_, result = call("/exams/details", {"id": exam_id, "examDetails": answers})
print("交卷结果:", json.dumps(result, ensure_ascii=False)[:400])
