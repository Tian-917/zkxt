# -*- coding: utf-8 -*-
"""用真正的 multipart 请求验证文件上传接口。"""
import json
import urllib.error
import urllib.request
import uuid

BASE = "http://192.168.150.101:10010"

body = json.dumps({"type": 1, "username": "jack", "password": "123456", "rememberMe": True}).encode()
req = urllib.request.Request(BASE + "/as/accounts/login", data=body, method="POST")
req.add_header("Content-Type", "application/json")
token = json.loads(urllib.request.urlopen(req, timeout=15).read())["data"]

boundary = "----zkxt" + uuid.uuid4().hex
content = (
    "--" + boundary + "\r\n"
    'Content-Disposition: form-data; name="file"; filename="t.txt"\r\n'
    "Content-Type: text/plain\r\n\r\n"
    "hello zkxt\r\n"
    "--" + boundary + "--\r\n"
).encode()

for path in ("/ms/files",):
    r = urllib.request.Request(BASE + path, data=content, method="POST")
    r.add_header("Content-Type", "multipart/form-data; boundary=" + boundary)
    r.add_header("authorization", token)
    try:
        with urllib.request.urlopen(r, timeout=25) as resp:
            print(path, "->", resp.status, resp.read().decode("utf-8", "ignore")[:200])
    except urllib.error.HTTPError as e:
        print(path, "->", e.code, e.read().decode("utf-8", "ignore")[:200])
