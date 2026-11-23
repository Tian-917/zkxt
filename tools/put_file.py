# -*- coding: utf-8 -*-
"""把本地文件写进容器（docker archive API）。

用法: python tools/put_file.py <容器名> <容器内目录> <本地文件> [容器内文件名]
"""
import io
import json
import os
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


container, dest_dir, local = sys.argv[1], sys.argv[2], sys.argv[3]
name = sys.argv[4] if len(sys.argv) > 4 else os.path.basename(local)
data = io.open(local, "rb").read()

buf = io.BytesIO()
with tarfile.open(fileobj=buf, mode="w") as tar:
    info = tarfile.TarInfo(name)
    info.size = len(data)
    info.mode = 0o755
    tar.addfile(info, io.BytesIO(data))

code, _ = api("PUT", "/containers/%s/archive?path=%s" % (container, dest_dir),
              raw=buf.getvalue(), ctype="application/x-tar")
print("写入:", code, "->", dest_dir + "/" + name)
