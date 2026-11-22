# -*- coding: utf-8 -*-
"""对 OpenAPI 文档里的 GET 接口做一次只读自测，输出状态码统计与异常清单。"""
import json
import re
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:10010"
DOC = "apifox/zkxt-openapi.json"


def login(username, password):
    body = json.dumps({"type": 1, "username": username, "password": password, "rememberMe": True}).encode()
    req = urllib.request.Request(BASE + "/as/accounts/login", data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return json.loads(r.read()).get("data")
    except Exception as e:
        print("登录失败", username, e)
        return None


def call(method, path, token):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("authorization", token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read().decode("utf-8", "ignore")[:200]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")[:200]
    except Exception as e:
        return 0, type(e).__name__


doc = json.load(open(DOC, encoding="utf-8"))
token = login("jack", "123456")
print("学生 token:", "有" if token else "无")

stats = {}
bad = []
total = 0
for path, ops in doc["paths"].items():
    for method, op in ops.items():
        if method != "get":
            continue
        # 路径参数用 1 占位
        real = re.sub(r"\{[^}]+\}", "1", path)
        code, body = call("GET", real, token)
        total += 1
        stats[code] = stats.get(code, 0) + 1
        # 业务码在 body 里
        biz = ""
        m = re.search(r'"code":\s*(\d+)', body)
        if m:
            biz = m.group(1)
        if code >= 500 or code == 0 or biz in ("500",):
            bad.append((code, biz, real, op["summary"][:20], body[:120].replace("\n", " ")))

print("GET 接口总数:", total)
print("状态码分布:", dict(sorted(stats.items())))
print("异常(5xx/连不通/业务500):", len(bad))
for b in bad[:20]:
    print("  ", b)
