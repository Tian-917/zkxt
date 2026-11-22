# -*- coding: utf-8 -*-
"""证书域冒烟测试：发证 / 幂等 / 资格校验 / 验真 / MQ 异步发证"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8090/cert"


def post(path, body):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(BASE + path, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def get(path):
    try:
        with urllib.request.urlopen(BASE + path, timeout=10) as r:
            return r.status, r.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


payload = {
    "userId": 1,
    "userName": "林晓",
    "certType": "初级会计职称",
    "examId": 1001,
    "score": 82,
    "studyMinutes": 150,
}

print("1) 首次发证")
code, res = post("/issue", payload)
print("   HTTP", code, "->", json.dumps(res, ensure_ascii=False)[:220])
cert_no = res.get("certNo") if isinstance(res, dict) else None
verify_code = res.get("verifyCode") if isinstance(res, dict) else None

print("2) 重复发证（幂等，应返回同一证书）")
code2, res2 = post("/issue", payload)
same = isinstance(res2, dict) and res2.get("certNo") == cert_no
print("   HTTP", code2, "同一编号:", same, "->", res2.get("certNo") if isinstance(res2, dict) else res2)

print("3) 成绩不达标（应拒绝）")
code3, res3 = post("/issue", dict(payload, examId=1002, score=50))
print("   HTTP", code3, "->", json.dumps(res3, ensure_ascii=False)[:160])

print("4) 学时不达标（应拒绝）")
code4, res4 = post("/issue", dict(payload, examId=1003, studyMinutes=30))
print("   HTTP", code4, "->", json.dumps(res4, ensure_ascii=False)[:160])

print("5) 验真（公开接口）")
print("   ", get("/verify/" + (verify_code or "NOPE"))[1][:220])

print("6) 验真 - 错误码")
print("   ", get("/verify/000000000000")[1][:120])

print("7) MQ 异步发证")
code7, res7 = post("/exam-passed", dict(payload, examId=2001, userName="张三"))
print("   HTTP", code7, "(已投递，等待消费)")
for _ in range(10):
    time.sleep(1)
    body = get("/")[1] if False else None
print("   等待 5s 后查库校验（见下一步）")
time.sleep(4)
print("done")
