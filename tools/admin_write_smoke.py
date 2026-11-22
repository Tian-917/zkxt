# -*- coding: utf-8 -*-
"""管理端写操作冒烟：新增题目 / 分类 / 优惠券 / 通知任务"""
import json
import urllib.error
import urllib.request
from datetime import datetime, timedelta

GW = "http://localhost:10010"


def call(method, path, body=None, token=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("authorization", token)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode("utf-8", "ignore")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        return {"http": e.code, "body": e.read().decode("utf-8", "ignore")[:200]}


token = call("POST", "/as/accounts/admin/login",
             {"type": 1, "username": "admin", "password": "123456", "rememberMe": True})["data"]
print("员工登录 OK")

stamp = datetime.now().strftime("%H%M%S")

# 1. 新增题目
print("\n[新增题目]")
q = call("POST", "/es/questions", {
    "name": f"自测题目 {stamp}：下列属于资产的是（ ）",
    "type": 2, "difficulty": 2, "score": 5,
    "cateIds": [], "options": ["库存现金", "应付账款", "预收账款", "短期借款"],
    "answer": "A", "analysis": "库存现金属于资产，其余三项均为负债类科目"
}, token)
print("  创建:", q.get("code"), q.get("msg"))
page = call("GET", "/es/questions/page?pageNo=1&pageSize=1", token=token).get("data") or {}
print("  题库总数:", page.get("total"))

# 2. 新增分类
print("\n[新增分类]")
c = call("POST", "/cs/categorys/add", {"parentId": 1004, "name": f"自测分类{stamp}", "index": 99}, token)
print("  创建:", c.get("code"), c.get("msg"))
cats = call("GET", "/cs/categorys/getAllOfOneLevel", token=token).get("data") or []
print("  分类总数:", len(cats), "| 含自测:", any(f"自测分类{stamp}" in str(x.get("name")) for x in cats))

# 3. 新增优惠券
print("\n[新增优惠券]")
cp = call("POST", "/prs/coupons", {
    "name": f"自测满减券{stamp}"[:20],
    "specific": False, "scopes": [],
    "discountType": 4, "thresholdAmount": 10000, "discountValue": 1000,
    "maxDiscountAmount": 0, "totalNum": 100, "userLimit": 1, "obtainWay": 1
}, token)
print("  创建:", cp.get("code"), cp.get("msg"))
cps = call("GET", "/prs/coupons/page?pageNo=1&pageSize=1", token=token).get("data") or {}
print("  券总数:", cps.get("total"))

# 4. 新增通知任务
print("\n[新增通知任务]")
tpl = call("GET", "/sms/notice-templates?pageNo=1&pageSize=5", token=token).get("data") or {}
tpls = tpl.get("list") or []
print("  可用模板:", [(t["id"], t.get("name")) for t in tpls][:2])
if tpls:
    push = (datetime.now() + timedelta(minutes=5)).strftime("%Y-%m-%d %H:%M:%S")
    nt = call("POST", "/sms/notice-tasks", {
        "templateId": tpls[0]["id"], "name": f"自测通知{stamp}",
        "partial": False, "pushTime": push, "maxTimes": 3, "interval": 60
    }, token)
    print("  创建:", nt.get("code"), nt.get("msg"), "| id:", nt.get("data"))
    tasks = call("GET", "/sms/notice-tasks?pageNo=1&pageSize=5", token=token).get("data") or {}
    print("  任务总数:", tasks.get("total"))
else:
    print("  没有可用模板，跳过")
