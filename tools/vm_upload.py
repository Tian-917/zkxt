# -*- coding: utf-8 -*-
"""把本地目录上传到虚拟机宿主机目录（通过 docker API 的 PUT /archive）。

用法: python tools/vm_upload.py <本地目录> <宿主机父目录> <目标子目录名>
例如: python tools/vm_upload.py web/dist /usr/local/src zkxt-portal
"""
import io
import json
import os
import sys
import tarfile
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:2375"


def api(method, path, body=None, ctype=None, raw=None):
    data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if ctype:
        req.add_header("Content-Type", ctype)
    elif body is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            payload = r.read()
            return r.status, payload
    except urllib.error.HTTPError as e:
        return e.code, e.read()


def main():
    local_dir, remote_parent, name = sys.argv[1], sys.argv[2], sys.argv[3]
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w") as tar:
        for root, _dirs, files in os.walk(local_dir):
            for f in files:
                full = os.path.join(root, f)
                rel = os.path.relpath(full, local_dir).replace("\\", "/")
                info = tar.gettarinfo(full, arcname=f"{name}/{rel}")
                info.mode = 0o644
                with open(full, "rb") as fh:
                    tar.addfile(info, fh)
    data = buf.getvalue()
    print(f"tar 大小 {len(data)//1024} KB -> {remote_parent}/{name}")

    cfg = {
        "Image": "redis",
        "Cmd": ["sh", "-c", "sleep 120"],
        "HostConfig": {"Binds": [f"{remote_parent}:/target"]},
    }
    code, body = api("POST", "/containers/create", cfg)
    cid = json.loads(body)["Id"]
    api("POST", f"/containers/{cid}/start")
    code, body = api("PUT", f"/containers/{cid}/archive?path=/target", raw=data, ctype="application/x-tar")
    print("extract:", code)
    api("POST", f"/containers/{cid}/stop?t=1")
    api("DELETE", f"/containers/{cid}?force=1")
    sys.exit(0 if code in (200, 204) else 1)


if __name__ == "__main__":
    main()
