# -*- coding: utf-8 -*-
"""通过远端 Docker API（tcp://192.168.150.101:2375）读容器挂载目录里的文件。
用法: python tools/vm_docker.py cat <镜像> <宿主目录> <容器内路径>
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:2375"


def api(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            raw = r.read()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        print("DOCKER API ERROR", e.code, e.read().decode("utf-8", "ignore")[:300])
        raise


def run(image, cmd, binds):
    cfg = {
        "Image": image,
        "Cmd": cmd,
        "HostConfig": {"Binds": binds, "AutoRemove": False},
    }
    c = api("POST", "/containers/create", cfg)
    cid = c["Id"]
    api("POST", f"/containers/{cid}/start")
    # 等它退出
    for _ in range(30):
        info = api("GET", f"/containers/{cid}/json")
        if not info["State"]["Running"]:
            break
        time.sleep(0.3)
    logs = api("GET", f"/containers/{cid}/logs?stdout=1&stderr=1").get("raw", b"")
    api("DELETE", f"/containers/{cid}?force=1")
    return logs


def logs_text(cid):
    # /logs 返回的是带 docker 流头的二进制，这里用带 stdout=1 的简单方式取文本
    req = urllib.request.urlopen(BASE + f"/containers/{cid}/logs?stdout=1&stderr=1&timestamps=0", timeout=60)
    raw = req.read()
    out = []
    i = 0
    while i + 8 <= len(raw):
        size = int.from_bytes(raw[i + 4:i + 8], "big")
        out.append(raw[i + 8:i + 8 + size])
        i += 8 + size
    return b"".join(out).decode("utf-8", "ignore")


def run2(image, cmd, binds):
    bind_list = [f"{k}:{v}" for k, v in binds.items()]
    host_cfg = {"Binds": bind_list}
    # VM_PID=host 时让容器共享宿主 PID 命名空间（给宿主进程发信号，比如 reload nginx）
    if os.environ.get("VM_PID") == "host":
        host_cfg["PidMode"] = "host"
        host_cfg["Privileged"] = True
    cfg = {"Image": image, "Cmd": cmd, "HostConfig": host_cfg}
    cid = api("POST", "/containers/create", cfg)["Id"]
    api("POST", f"/containers/{cid}/start")
    for _ in range(40):
        if not api("GET", f"/containers/{cid}/json")["State"]["Running"]:
            break
        time.sleep(0.3)
    text = logs_text(cid)
    api("DELETE", f"/containers/{cid}?force=1")
    return text


if __name__ == "__main__":
    if len(sys.argv) >= 5 and sys.argv[1] == "cat":
        print(run2(sys.argv[2], ["cat", sys.argv[4]], {sys.argv[3]: "/mnt"}))
    elif len(sys.argv) >= 4 and sys.argv[1] == "ls":
        print(run2(sys.argv[2], ["ls", "-la", sys.argv[3]], {}))
    else:
        print(__doc__)
