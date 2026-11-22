# -*- coding: utf-8 -*-
"""完整支付链路冒烟（无需商户凭证）：
预下单 -> 下单 -> 申请支付(渠道 mock) -> 模拟支付成功 -> 校验订单状态与课表
"""
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
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        print("HTTP", e.code, e.read().decode("utf-8", "ignore")[:200])
        return None


token = call("POST", "/as/accounts/login",
             {"type": 1, "username": "jack", "password": "123456", "rememberMe": True})["data"]

COURSE_ID = 5
print("1) 预下单 course=", COURSE_ID)
pre = call("GET", "/ts/orders/prePlaceOrder?courseIds=%d" % COURSE_ID, token=token)["data"]
order_id = pre["orderId"]
print("   订单id =", order_id, "应付 =", pre.get("payAmount"), "分")

print("2) 提交订单")
placed = call("POST", "/ts/orders/placeOrder",
              {"orderId": order_id, "courseIds": [COURSE_ID], "couponIds": []}, token)["data"]
print("   状态 =", placed.get("status"), "应付 =", placed.get("payAmount"))

print("3) 申请支付（渠道 mock）")
url = call("POST", "/ts/pay/order", {"orderId": order_id, "payChannelCode": "mock"}, token)["data"]
print("   支付链接 =", url)

print("4) 模拟支付成功")
ok = call("POST", "/ps/pay-orders/%s/mock-pay" % order_id, token=token)["data"]
print("   状态流转 =", ok)

import time
time.sleep(2)
print("5) 校验")
st = call("GET", "/ts/orders/%s/status" % order_id, token=token)
print("   订单状态 =", json.dumps(st, ensure_ascii=False)[:160])
lesson = call("GET", "/ls/learning-records/course/%s" % COURSE_ID, token=token)
print("   课表(报名是否生效) =", "已报名 id=" + str(lesson["data"]["id"]) if lesson.get("data") else "未报名")
