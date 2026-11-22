# -*- coding: utf-8 -*-
"""验证：新建课程 -> 保存目录(step=2) -> 上架，看目录是否真的落库"""
import io
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
        with urllib.request.urlopen(req, timeout=40) as r:
            raw = r.read().decode("utf-8", "ignore")
            return {"status": r.status, "json": json.loads(raw) if raw else {}}
    except urllib.error.HTTPError as e:
        return {"status": e.code, "json": {"body": e.read().decode("utf-8", "ignore")[:300]}}


token = call("POST", "/as/accounts/admin/login",
             {"type": 1, "username": "admin", "password": "123456", "rememberMe": True})["json"]["data"]
cats = call("GET", "/cs/categorys/getAllOfOneLevel", None, token)["json"]["data"]
c3 = [c for c in cats if c.get("level") == 3][0]
end = (datetime.now() + timedelta(days=90)).strftime("%Y-%m-%d %H:%M:%S")

created = call("POST", "/cs/courses/baseInfo/save", {
    "name": "E2E课程目录" + datetime.now().strftime("%H%M%S"),
    "thirdCateId": c3["id"], "coverUrl": "/img-tx/default-cover-url.jpg",
    "free": False, "price": 19900, "purchaseEndTime": end,
    "introduce": "介绍", "usePeople": "零基础", "detail": "详情", "validDuration": 12
}, token)
cid = (created.get("json", {}).get("data") or {}).get("id")

catas = call("POST", f"/cs/courses/catas/save/{cid}/2", [{
    "type": 1, "name": "第1章 入门", "index": 1,
    "sections": [{"type": 2, "name": "1.1 课程导学", "index": 1}]
}], token)

up = call("POST", "/cs/courses/upShelf", {"id": cid}, token)

io.open("cata.json", "w", encoding="utf-8").write(json.dumps({
    "新建课程": created, "保存目录": catas, "上架": up
}, ensure_ascii=False, indent=1))
print("done, courseId=", cid)
