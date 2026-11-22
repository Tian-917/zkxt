# -*- coding: utf-8 -*-
"""后端接口冒烟：拿 jack 的 token 逐个打主要 GET 接口，看返回 code"""
import json
import urllib.error
import urllib.request

GW = "http://192.168.150.101:10010"


def call(method, path, body=None, token=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(GW + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("authorization", token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            raw = r.read().decode("utf-8", "ignore")
            return r.status, raw
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")
    except Exception as e:
        return -1, str(e)


status, raw = call("POST", "/as/accounts/login",
                   {"type": 1, "username": "jack", "password": "123456", "rememberMe": True})
token = json.loads(raw)["data"]

TARGETS = [
    ("auth   ", "GET", "/as/menus/parent/0"),
    ("user   ", "GET", "/us/users/2"),
    ("search ", "GET", "/ss/courses/portal?pageNo=1&pageSize=2"),
    ("search ", "GET", "/ss/recommend/new"),
    ("course ", "GET", "/cs/courses/catas/1"),
    ("course ", "GET", "/cs/courses/statistics"),
    ("course ", "GET", "/cs/course/1?withCatalogue=true&withTeachers=true"),
    ("learn  ", "GET", "/ls/learning-records/course/1"),
    ("learn  ", "GET", "/ls/lessons/page?pageNo=1&pageSize=2"),
    ("learn  ", "GET", "/ls/points/today"),
    ("learn  ", "GET", "/ls/sign-records"),
    ("learn  ", "GET", "/ls/boards/seasons/list"),
    ("learn  ", "GET", "/ls/boards"),
    ("exam   ", "GET", "/es/wrong/list?mastered=0"),
    ("exam   ", "GET", "/es/exams/page?pageNo=1&pageSize=2"),
    ("cert   ", "GET", "/cts/cert/my"),
    ("trade  ", "GET", "/ts/orders/page?pageNo=1&pageSize=2"),
    ("trade  ", "GET", "/ts/pay/channels"),
    ("promo  ", "GET", "/prs/coupons/page?pageNo=1&pageSize=2"),
    ("promo  ", "GET", "/prs/user-coupons/page?pageNo=1&pageSize=2&status=1"),
    ("pay    ", "GET", "/ps/pay-channels/list"),
    ("remark ", "GET", "/rs/likes/list?bizIds=1&bizIds=2"),
    ("media  ", "GET", "/ms/medias/signature/play?sectionId=3"),
    ("message", "GET", "/sms/inboxes?pageNo=1&pageSize=2"),
    ("data   ", "GET", "/ds/data/today"),
]

ok = bad = 0
for name, method, path in TARGETS:
    st, body = call(method, path, token=token)
    try:
        code = json.loads(body).get("code")
        msg = (json.loads(body).get("msg") or "")[:28]
    except Exception:
        code, msg = None, body[:28]
    flag = "OK  " if code == 200 else "FAIL"
    if code == 200:
        ok += 1
    else:
        bad += 1
    print("%s %s %-46s http=%s code=%s %s" % (flag, name, path[:46], st, code, msg))

print("\n通过 %d / 失败 %d" % (ok, bad))
