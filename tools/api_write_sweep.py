# -*- coding: utf-8 -*-
"""扫描所有写接口：按 schema 自动造合法请求体，分类统计结果。"""
import io
import json
import re
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:10010"
DOC = "apifox/zkxt-openapi.json"
DOCJ = json.load(open(DOC, encoding="utf-8"))
SCHEMAS = DOCJ.get("components", {}).get("schemas", {})


def sample(schema, depth=0):
    if depth > 3 or not isinstance(schema, dict):
        return None
    if "$ref" in schema:
        name = schema["$ref"].split("/")[-1]
        return sample(SCHEMAS.get(name, {}), depth + 1)
    t = schema.get("type")
    if t == "array":
        return [sample(schema.get("items", {}), depth + 1)]
    if t == "object":
        return {k: sample(v, depth + 1) for k, v in (schema.get("properties") or {}).items()}
    if t == "integer":
        return 1
    if t == "number":
        return 1.0
    if t == "boolean":
        return True
    if t == "string":
        fmt = schema.get("format")
        if fmt == "date-time":
            return "2026-11-22 12:00:00"
        if fmt == "date":
            return "2026-11-22"
        return "test"
    return None


def login():
    body = json.dumps({"type": 1, "username": "jack", "password": "123456", "rememberMe": True}).encode()
    req = urllib.request.Request(BASE + "/as/accounts/login", data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=20) as r:
        return json.loads(r.read())["data"]


def call(method, path, token, payload=None):
    data = json.dumps(payload).encode() if payload is not None else b""
    req = urllib.request.Request(BASE + path, data=data if method != "DELETE" else None, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("authorization", token)
    try:
        with urllib.request.urlopen(req, timeout=25) as r:
            return r.status, r.read().decode("utf-8", "ignore")[:160]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")[:160]
    except Exception as e:
        return 0, type(e).__name__


token = login()
buckets, fails, warns, results = {}, [], [], []
total = 0
for path, ops in DOCJ["paths"].items():
    for method, op in ops.items():
        if method == "get":
            continue
        # 跳过 DELETE：会真的删数据（上一轮把种子视频删了，导致试看挂掉）
        if method == "delete":
            continue
        # 跳过登录/登出（已单独测）和角色菜单这类会破坏权限的
        if any(k in path for k in ("/accounts/login", "/accounts/logout", "/menus", "/privileges", "/roles")):
            continue
        real = re.sub(r"\{[^}]+\}", "1", path)
        schema = ((op.get("requestBody") or {}).get("content", {}).get("application/json", {}) or {}).get("schema")
        payload = sample(schema) if schema else None
        if isinstance(payload, dict) and not payload:
            payload = None
        code, body = call(method.upper(), real, token, payload)
        total += 1
        biz = (re.search(r'"code":\s*(\d+)', body) or [None, ""])[1]
        results.append([method.upper(), real, op["summary"][:20], code, biz])
        key = "%s/%s" % (code, biz)
        buckets[key] = buckets.get(key, 0) + 1
        if code >= 500 or code == 0 or biz == "500":
            fails.append((method.upper(), real, op["summary"][:16], code, biz, body[:110].replace("\n", " ")))
        elif code == 404 or biz in ("401", "403"):
            warns.append((method.upper(), real, op["summary"][:14], code, biz))

import json as _json
_json.dump(results, io.open("apifox/write-sweep-result.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
print("写接口测试数:", total)
print("结果分布:", dict(sorted(buckets.items())))
print("\n=== 5xx / 异常（真 bug）:", len(fails), "===")
for f in fails:
    print("  ", f)
print("\n=== 404/401/403（需权限或数据不存在）:", len(warns), "===")
for w in warns[:8]:
    print("  ", w)
