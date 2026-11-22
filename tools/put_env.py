# -*- coding: utf-8 -*-
"""把 env 文件直接写进 Jenkins 容器（docker archive API）。

用法: python tools/put_env.py <容器名> <容器内目录> <key=value> ...
"""
import io
import json
import sys
import tarfile
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:2375"


def api(method, path, raw=None, ctype=None):
    req = urllib.request.Request(BASE + path, data=raw, method=method)
    if ctype:
        req.add_header("Content-Type", ctype)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()


container, dest_dir = sys.argv[1], sys.argv[2]
lines = sys.argv[3:]

buf = io.BytesIO()
content = ("\n".join(lines) + "\n").encode()
with tarfile.open(fileobj=buf, mode="w") as tar:
    info = tarfile.TarInfo("zkxt.env")
    info.size = len(content)
    info.mode = 0o600
    tar.addfile(info, io.BytesIO(content))

code, _ = api("PUT", "/containers/%s/archive?path=%s" % (container, dest_dir),
              raw=buf.getvalue(), ctype="application/x-tar")
print("写入:", code, "->", dest_dir + "/zkxt.env")
