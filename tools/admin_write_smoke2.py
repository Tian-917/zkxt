# -*- coding: utf-8 -*-
"""管理端写操作冒烟（第二轮）：编辑题目/编辑券/发放/暂停/分类编辑启停/新建课程+目录+上架"""
import io
import json
import urllib.error
import urllib.request
from datetime import datetime, timedelta

GW = "http://localhost:10010"
out = []


def call(method, path, body=None, token=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("authorization", token)
    try:
        with urllib.request.urlopen(req, timeout=40) as r:
            raw = r.read().decode("utf-8", "ignore")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        return {"http": e.code, "body": e.read().decode("utf-8", "ignore")[:300]}


def rec(name, resp, extra=None):
    item = {"step": name, "code": resp.get("code"), "msg": resp.get("msg")}
    if extra:
        item.update(extra)
    out.append(item)
    print(name, item["code"], item["msg"])


token = call("POST", "/as/accounts/admin/login",
             {"type": 1, "username": "admin", "password": "123456", "rememberMe": True})["data"]

# ---------- 1. 编辑题目 ----------
qs = call("GET", "/es/questions/page?pageNo=1&pageSize=1", token=token).get("data", {}).get("list", [])
if qs:
    qid = qs[0]["id"]
    detail = call("GET", f"/es/questions/{qid}", token=token).get("data") or {}
    rec("题目详情", {"code": 200, "msg": f"cateId1={detail.get('cateId1')} cateId3={detail.get('cateId3')}"},
        {"name": detail.get("name")})
    # 用一个合法三级分类覆盖（旧数据 cateId3 可能是 0）
    cats = call("GET", "/cs/categorys/getAllOfOneLevel", token=token).get("data") or []
    byid = {c["id"]: c for c in cats}
    leaf = [c for c in cats if c.get("level") == 3][0]
    chain, cur, guard = [], leaf, 0
    while cur and guard < 10:
        chain.insert(0, cur["id"])
        cur = byid.get(cur.get("parentId"))
        guard += 1
    r = call("PUT", f"/es/questions/{qid}", {
        "name": detail.get("name"), "type": detail.get("type") or 2,
        "difficulty": detail.get("difficulty") or 2, "score": detail.get("score") or 5,
        "cateIds": chain[:3], "options": detail.get("options") or [],
        "answer": detail.get("answer") or "A",
        "analysis": detail.get("analysis") or "编辑题目时的解析占位内容"
    }, token)
    rec("编辑题目", r)

# ---------- 2. 优惠券：编辑 / 发放 / 暂停 ----------
cps = call("GET", "/prs/coupons/page?pageNo=1&pageSize=1", token=token).get("data", {}).get("list", [])
if cps:
    c = cps[0]
    r = call("PUT", f"/prs/coupons/{c['id']}", {
        "name": c["name"], "specific": False, "scopes": [],
        "discountType": (c.get("discountType") or {}).get("value") if isinstance(c.get("discountType"), dict) else c.get("discountType"),
        "thresholdAmount": c.get("thresholdAmount"), "discountValue": c.get("discountValue"),
        "maxDiscountAmount": c.get("maxDiscountAmount"), "totalNum": c.get("totalNum") or 100,
        "userLimit": c.get("userLimit") or 1,
        "obtainWay": (c.get("obtainWay") or {}).get("value") if isinstance(c.get("obtainWay"), dict) else (c.get("obtainWay") or 1)
    }, token)
    rec("编辑优惠券", r)
    now = datetime.now() + timedelta(minutes=2)
    r = call("PUT", f"/prs/coupons/{c['id']}/issue", {
        "issueBeginTime": now.strftime("%Y-%m-%d %H:%M:%S"),
        "issueEndTime": (now + timedelta(days=7)).strftime("%Y-%m-%d %H:%M:%S"),
        "termDays": 30
    }, token)
    rec("发放优惠券", r)
    r = call("PUT", f"/prs/coupons/{c['id']}/pause", None, token)
    rec("暂停发放", r)

# ---------- 3. 分类：编辑 / 启停 ----------
cats = call("GET", "/cs/categorys/getAllOfOneLevel", token=token).get("data") or []
target = next((x for x in cats if "E2E" in str(x.get("name")) or "UI自测" in str(x.get("name"))), None)
if target:
    r = call("PUT", "/cs/categorys/update", {"id": target["id"], "name": target["name"], "index": target.get("index") or 1}, token)
    rec("编辑分类", r)
    r = call("PUT", "/cs/categorys/disableOrEnable", {"id": target["id"], "status": 0}, token)
    rec("禁用分类", r)
    r = call("PUT", "/cs/categorys/disableOrEnable", {"id": target["id"], "status": 1}, token)
    rec("启用分类", r)

# ---------- 4. 新建课程 + 目录 + 上架 ----------
stamp = datetime.now().strftime("%H%M%S")
cat3 = [c for c in cats if c.get("level") == 3][0]
end = (datetime.now() + timedelta(days=90)).strftime("%Y-%m-%d %H:%M:%S")
r = call("POST", "/cs/courses/baseInfo/save", {
    "name": f"E2E课程{stamp}",
    "thirdCateId": cat3["id"],
    "coverUrl": "/img-tx/default-cover-url.jpg",
    "free": False,
    "price": 19900,
    "purchaseEndTime": end,
    "introduce": "自动化测试创建的课程",
    "usePeople": "零基础学员",
    "detail": "包含课程与题库",
    "validDuration": 12
}, token)
rec("新建课程", r, {"新课程id": (r.get("data") or {}).get("id")})
new_id = (r.get("data") or {}).get("id")
if new_id:
    r = call("POST", f"/cs/courses/catas/save/{new_id}", [{
        "type": 1, "name": "第1章 入门", "index": 1,
        "sections": [{"type": 2, "name": "1.1 课程导学", "index": 1}]
    }], token)
    rec("保存目录", r)
    r = call("POST", "/cs/courses/upShelf", {"id": new_id}, token)
    rec("尝试上架", r)

io.open("w2.json", "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))
print("done")
