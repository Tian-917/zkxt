# -*- coding: utf-8 -*-
"""试看/购买的服务端兜底校验（服务跑在 VM 容器里，数据从库里动态取）。

1. 未购买用户请求「试看节」播放签名 -> 200
2. 未购买用户请求「锁定节」播放签名 -> 403 / 提示不支持试看
3. 用别人的 lessonId 写学习进度 -> 被拒
4. 本人写自己的学习进度 -> 200
"""
import json
import re
import subprocess
import time
import urllib.error
import urllib.request

GW = "http://192.168.150.101:10010"
DOCKER = "http://192.168.150.101:2375"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
out = []


def sql(q):
    r = subprocess.run([MYSQL, "-h", "192.168.150.101", "-uroot", "-p123", "-N", "-B",
                        "--default-character-set=utf8mb4", "-e", q],
                       capture_output=True, text=True, encoding="utf-8", errors="ignore")
    return [line.split("\t") for line in r.stdout.strip().splitlines() if line.strip()]


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
        raw = e.read().decode("utf-8", "ignore")
        try:
            return json.loads(raw)
        except Exception:
            return {"http": e.code, "raw": raw[:200]}


def rec(name, ok, detail=""):
    out.append({"step": name, "ok": bool(ok), "detail": detail})
    print("%s %s  %s" % ("PASS" if ok else "FAIL", name, detail))


def vm_logs(container, tail=300):
    try:
        with urllib.request.urlopen(f"{DOCKER}/containers/{container}/logs?stdout=1&stderr=1&tail={tail}", timeout=20) as r:
            raw = r.read()
        buf, i, text = [], 0, ""
        while i + 8 <= len(raw):
            n = int.from_bytes(raw[i + 4:i + 8], "big")
            buf.append(raw[i + 8:i + 8 + n])
            i += 8 + n
        text = b"".join(buf).decode("utf-8", "ignore") if buf else raw.decode("utf-8", "ignore")
        return text
    except Exception:
        return ""


# 从库里动态挑小节：有 trailer=1 的、有 trailer=0 的（且都配了视频）
rows = sql("select c.id from zk_course.course_catalogue c join zk_course.course ca on ca.id = c.course_id "
           "where c.trailer = 1 and c.media_id > 0 and c.type = 2 and c.parent_catalogue_id > 0 order by c.id limit 1")
trial = int(rows[0][0]) if rows else None
rows = sql("select c.id from zk_course.course_catalogue c join zk_course.course ca on ca.id = c.course_id "
           "where c.trailer = 0 and c.media_id > 0 and c.type = 2 and c.parent_catalogue_id > 0 order by c.id limit 1")
locked = int(rows[0][0]) if rows else None
print("试看小节:", trial, "| 锁定小节:", locked)

# 造一个全新学员
phone = "139" + str(int(time.time()))[-8:]
call("POST", f"/us/users/code?phone={phone}")
time.sleep(2)
m = re.findall(r"(\d{6})", vm_logs("zkxt-user"))
code = m[-1] if m else None
print("验证码:", code, "| 手机号:", phone)
call("POST", "/us/students/register", {"cellPhone": phone, "password": "trial123", "code": code})
tk = call("POST", "/as/accounts/login",
          {"type": 1, "cellPhone": phone, "password": "trial123", "rememberMe": True}).get("data")
print("新学员 token:", "有" if tk else "无")

# 1. 试看节
r1 = call("GET", f"/ms/medias/signature/play?sectionId={trial}", token=tk)
rec("未购买 → 试看节可拿播放签名", r1.get("code") == 200, f"code={r1.get('code')} msg={r1.get('msg')}")

# 2. 锁定节
r2 = call("GET", f"/ms/medias/signature/play?sectionId={locked}", token=tk)
rec("未购买 → 锁定节被拒", r2.get("code") in (403, 401) or "试看" in str(r2.get("msg")),
    f"code={r2.get('code')} msg={r2.get('msg')}")

# 3/4. 学习进度
jack = call("POST", "/as/accounts/login",
            {"type": 1, "username": "jack", "password": "123456", "rememberMe": True}).get("data")
mine = (call("GET", "/ls/lessons/page?pageNo=1&pageSize=1", token=jack).get("data") or {}).get("list") or []
lesson_id = mine[0]["id"] if mine else None
payload = {"sectionType": 1, "lessonId": lesson_id, "sectionId": trial,
           "duration": 1620, "moment": 100, "commitTime": "2026-11-22 12:00:00"}
r3 = call("POST", "/ls/learning-records", payload, tk)
# 越权被拒：业务层返回 400/403，或网关直接 401/403（此时响应体为空）
rejected = r3.get("code") in (400, 403) or r3.get("http") in (401, 403)
rec("越权写别人课表进度被拒", rejected, f"code={r3.get('code')} http={r3.get('http')} msg={r3.get('msg')}")
r4 = call("POST", "/ls/learning-records", payload, jack)
rec("本人写自己课表进度正常", r4.get("code") == 200, f"code={r4.get('code')} msg={r4.get('msg')}")

ok = sum(1 for x in out if x["ok"])
print("\n校验：%d 项，通过 %d，失败 %d" % (len(out), ok, len(out) - ok))
