# -*- coding: utf-8 -*-
"""把新 jar 覆盖进运行中的容器并重启（验证用，正式走 Jenkins）。

用法: python tools/vm_hotfix_jar.py <容器名> <本地jar>
"""
import io
import json
import sys
import tarfile
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:2375"


def api(method, path, body=None, raw=None, ctype=None):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if ctype:
        req.add_header("Content-Type", ctype)
    elif body is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


container, jar_path = sys.argv[1], sys.argv[2]

buf = io.BytesIO()
with tarfile.open(fileobj=buf, mode="w") as tar:
    info = tarfile.TarInfo("app.jar")
    data = open(jar_path, "rb").read()
    info.size = len(data)
    info.mode = 0o644
    tar.addfile(info, io.BytesIO(data))

print("覆盖 jar:", api("PUT", "/containers/%s/archive?path=/app" % container,
                       raw=buf.getvalue(), ctype="application/x-tar")[0])
print("重启:", api("POST", "/containers/%s/restart?t=10" % container)[0])
