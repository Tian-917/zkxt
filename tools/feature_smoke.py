# -*- coding: utf-8 -*-
"""笔记 / 问答 / 退款 / 优惠券 四条链路的接口冒烟"""
import json
import urllib.error
import urllib.request

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
        return {"http": e.code, "body": e.read().decode("utf-8", "ignore")[:160]}


token = call("POST", "/as/accounts/login",
             {"type": 1, "username": "jack", "password": "123456", "rememberMe": True})["data"]
print("登录 OK")

# 取课程 1 的第一章第一节
catas = call("GET", "/cs/courses/catas/1")["data"]
ch = catas[0]
sec = ch["sections"][0]
chapter_id, section_id = ch["id"], sec["id"]
print("课程1 -> 章", chapter_id, "节", section_id, sec["name"])

# ---------- 1. 笔记 ----------
print("\n[笔记]")
r = call("POST", "/ls/notes", {"content": "自测笔记：80% 判定用的是 moment * 1.25 >= duration",
                               "noteMoment": 0, "isPrivate": False,
                               "courseId": 1, "chapterId": chapter_id, "sectionId": section_id}, token)
print("  发布:", r.get("code"), r.get("msg"))
page = call("GET", "/ls/notes/page?courseId=1&onlyMine=true", token=token).get("data") or {}
notes = page.get("list") or []
print("  我的笔记条数:", len(notes))
if notes:
    nid = notes[0]["id"]
    print("  收藏:", call("POST", f"/ls/notes/gathers/{nid}", token=token).get("code"))
    print("  取消收藏:", call("DELETE", f"/ls/notes/gathers/{nid}", token=token).get("code"))

# ---------- 2. 问答 ----------
print("\n[问答]")
r = call("POST", "/ls/questions", {"courseId": 1, "chapterId": chapter_id, "sectionId": section_id,
                                   "title": "自测提问：学完后多久能参加考试？", "description": "想确认一下流程",
                                   "anonymity": False}, token)
print("  提问:", r.get("code"), r.get("msg"))
qpage = call("GET", f"/ls/questions/page?courseId=1&sectionId={section_id}", token=token).get("data") or {}
qs = qpage.get("list") or []
print("  问题条数:", len(qs))
if qs:
    qid = qs[0]["id"]
    r = call("POST", "/ls/replies", {"questionId": qid, "content": "学时满 120 分钟 + 考试 60 分即可",
                                     "anonymity": False, "isStudent": True}, token)
    print("  回答:", r.get("code"), r.get("msg"))
    rp = call("GET", f"/ls/replies/page?questionId={qid}", token=token).get("data") or {}
    print("  回答条数:", len(rp.get("list") or []))

# ---------- 3. 优惠券 ----------
print("\n[优惠券]")
courses = [{"id": 1, "cateId": catas[0].get("id"), "price": 69900}]
r = call("POST", "/prs/user-coupons/available", courses, token)
data = r.get("data")
print("  可用方案:", len(data) if isinstance(data, list) else r.get("msg"))
if isinstance(data, list) and data:
    print("  首个方案已减:", data[0]["discountAmount"], "分, 券ids:", data[0]["ids"])

# ---------- 4. 退款 ----------
print("\n[退款]")
orders = call("GET", "/ts/orders/page?pageNo=1&pageSize=10", token=token).get("data") or {}
paid_detail = None
for o in orders.get("list") or []:
    if o.get("status") in (2, 4):
        for d in o.get("details") or []:
            if d.get("canRefund"):
                paid_detail = (o, d)
                break
    if paid_detail:
        break
if not paid_detail:
    print("  没有可退款的订单明细（可能已申请过）")
else:
    o, d = paid_detail
    print("  订单", o["id"], "明细", d["id"], d.get("name"))
    r = call("POST", "/ts/refund-apply", {"orderDetailId": d["id"], "refundReason": "自测退款",
                                          "questionDesc": "自动化验证退款链路"}, token)
    print("  申请退款:", r.get("code"), r.get("msg"))
    rp = call("GET", "/ts/refund-apply/page?pageNo=1&pageSize=5", token=token).get("data") or {}
    print("  我的退款申请:", len(rp.get("list") or []))
