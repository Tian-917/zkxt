# -*- coding: utf-8 -*-
"""给演示账号 jack(user 2) 造点数据：一次练习错题 + 一张证书

走真实接口（登录拿 token 之后带 authorization 调网关），不是直接改库。
"""
import json
import urllib.request

GW = "http://localhost:10010"


def post(path, body=None, token=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("authorization", token)
    with urllib.request.urlopen(req, timeout=30) as r:
        raw = r.read().decode("utf-8")
        return json.loads(raw) if raw else None


# 1. 登录
login = post("/as/accounts/login", {"type": 1, "username": "jack", "password": "123456", "rememberMe": True})
token = login["data"]
print("登录 OK，token 长度", len(token))

# 2. 练习卷全选 A -> 产生错题
exam = post("/es/exams", {"courseId": 1, "sectionId": 3, "type": 1}, token)["data"]
qs = exam["questions"]
detail = post("/es/exams/details", {
    "id": exam["id"],
    "examDetails": [{"questionId": str(q["id"]), "answer": "A"} for q in qs]
}, token)["data"]
print("练习卷 %d 题，答对 %d，错题已归档" % (len(qs), sum(1 for d in detail if d["correct"])))

# 3. 签一张证书（user 2 是学员，走 /cert/issue 内部接口）
cert = post("/cts/cert/issue", {
    "userId": 2, "userName": "张三", "certType": "初级会计职称",
    "examId": 500001, "score": 88, "studyMinutes": 220
}, token)
print("证书:", cert["data"]["certNo"] if cert.get("data") else cert)

print("\n现在用 jack 登录前端：/wrong 有错题，/cert 有一张证")
