# -*- coding: utf-8 -*-
"""兑换码全链路：建券(发放兑换码) -> 发放(自动生成码) -> 查码 -> 学员兑换 -> 券到账"""
import io
import json
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta

GW = "http://192.168.150.101:10010"
out = []


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
        return {"http": e.code, "body": e.read().decode("utf-8", "ignore")[:300]}


def rec(name, resp, extra=None):
    item = {"step": name, "code": resp.get("code"), "msg": resp.get("msg")}
    if extra:
        item.update(extra)
    out.append(item)
    print(name, item["code"], item["msg"])


admin = call("POST", "/as/accounts/admin/login",
             {"type": 1, "username": "admin", "password": "123456", "rememberMe": True})["data"]
stu = call("POST", "/as/accounts/login",
           {"type": 1, "username": "jack", "password": "123456", "rememberMe": True})["data"]

stamp = datetime.now().strftime("%H%M%S")
# 1. 建一张「发放兑换码」的券
created = call("POST", "/prs/coupons", {
    "name": f"码券{stamp}",
    "specific": False,
    "scopes": [],
    "discountType": 4,
    "thresholdAmount": 10000,
    "discountValue": 2000,
    "maxDiscountAmount": 0,
    "totalNum": 5,
    "userLimit": 1,
    "obtainWay": 2            # 2 = 发放兑换码
}, admin)
rec("建券(发放兑换码)", created)

cps = call("GET", "/prs/coupons/page?pageNo=1&pageSize=50", token=admin).get("data", {}).get("list", [])
target = next((c for c in cps if c["name"] == f"码券{stamp}"), None)
cid = target["id"] if target else None
rec("查到新券", {"code": 200 if cid else 0, "msg": str(cid)}, {"couponId": cid})

if cid:
    # 2. 发放 -> 自动生成兑换码
    begin = (datetime.now() + timedelta(seconds=20)).strftime("%Y-%m-%d %H:%M:%S")
    end = (datetime.now() + timedelta(days=7)).strftime("%Y-%m-%d %H:%M:%S")
    r = call("PUT", f"/prs/coupons/{cid}/issue", {
        "issueBeginTime": begin, "issueEndTime": end, "termDays": 30
    }, admin)
    rec("发放(触发生成码)", r)
    time.sleep(4)
    # 等 couponIssueJobHandler 把券切成“发放中”并写入缓存（任务 10s 一次）
    # couponIssueJobHandler 每 120 秒一次：轮询等券变成「发放中」(status=3) 且写入缓存
    print("等待发券定时任务切换状态（最多 160 秒）...")
    began = False
    for _ in range(16):
        time.sleep(10)
        page = call("GET", "/prs/coupons/page?pageNo=1&pageSize=50", token=admin)
        cur = next((x for x in page.get("data", {}).get("list", []) if x["id"] == cid), None)
        st = cur.get("status") if cur else None
        st_value = st.get("value") if isinstance(st, dict) else st
        if st_value == 3:
            began = True
            print("  券已切换为发放中")
            break
    rec("定时任务切换为发放中", {"code": 200 if began else 0, "msg": "status=3" if began else "仍在待发放"})

    # 3. 查兑换码
    codes = call("GET", f"/prs/codes/page?pageNo=1&pageSize=20&status=1&couponId={cid}", token=admin)
    lst = codes.get("data", {}).get("list", [])
    rec("查询兑换码", {"code": 200 if lst else 0, "msg": f"{len(lst)} 个"},
        {"sample": lst[0]["code"] if lst else None})

    if lst:
        code = lst[0]["code"]
        # 4. 学员兑换
        r = call("POST", f"/prs/user-coupons/{code}/exchange", None, stu)
        rec("学员兑换", r, {"code": code})

        # 5. 我的优惠券里应出现（异步落库，轮询最多 15 秒）
        arrived = False
        names = []
        for _ in range(15):
            mine = call("GET", "/prs/user-coupons/page?pageNo=1&pageSize=30&status=1", token=stu)
            names = [x.get("name") for x in mine.get("data", {}).get("list", [])]
            if f"码券{stamp}" in names:
                arrived = True
                break
            time.sleep(1)
        rec("券已到账", {"code": 200 if arrived else 0,
                        "msg": f"我的未使用券 {len(names)} 张，含新券={arrived}"})

        # 6. 重复兑换应失败
        again = call("POST", f"/prs/user-coupons/{code}/exchange", None, stu)
        rec("重复兑换被拦截", {"code": 200 if again.get("code") != 200 else 0,
                             "msg": again.get("msg")})

io.open("code.json", "w", encoding="utf-8").write(json.dumps(out, ensure_ascii=False, indent=1))
print("done")
