# -*- coding: utf-8 -*-
"""用 Gogs 网页登录流程验证账号，并（登录后）创建仓库。
用法: python tools/gogs_login.py [repoName]
"""
import http.cookiejar
import re
import sys
import urllib.parse
import urllib.request

BASE = "http://192.168.150.101:10880"
USER, PWD, EMAIL = "zkxt", "Gogs", "zkxt@zkxt.local"

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
opener.addheaders = [("User-Agent", "zkxt-ci")]


def get(path):
    with opener.open(BASE + path, timeout=20) as r:
        return r.status, r.read().decode("utf-8", "ignore"), r.geturl()


def post(path, data):
    body = urllib.parse.urlencode(data).encode()
    req = urllib.request.Request(BASE + path, data=body, method="POST")
    req.add_header("Content-Type", "application/x-www-form-urlencoded")
    try:
        with opener.open(req, timeout=20) as r:
            return r.status, r.read().decode("utf-8", "ignore"), r.geturl()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore"), ""


code, html, url = get("/user/login")
m = re.search(r'name="_csrf" value="([^"]+)"', html)
csrf = m.group(1) if m else ""
print("login page:", code, "csrf:", csrf[:12])

code, html, url = post("/user/login", {"_csrf": csrf, "user_name": USER, "password": PWD, "remember": "on"})
print("login post:", code, "->", url.replace(BASE, "")[:60])
ok = "/user/login" not in url
print("LOGIN", "OK" if ok else "FAIL")

if ok and len(sys.argv) > 1:
    repo = sys.argv[1]
    code, html, url = get("/repo/create")
    m = re.search(r'name="_csrf" value="([^"]+)"', html)
    csrf = m.group(1) if m else ""
    code, html, url = post("/repo/create", {
        "_csrf": csrf, "user_id": "2", "repo_name": repo,
        "description": "职考云 · 职业技能考证平台", "private": "", "auto_init": "",
    })
    print("create repo:", code, "->", url.replace(BASE, "")[:80])
