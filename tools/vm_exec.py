# -*- coding: utf-8 -*-
"""在远端容器里执行命令。用法: python tools/vm_exec.py <容器名> "<shell命令>" """
import json
import sys
import urllib.request

BASE = "http://192.168.150.101:2375"


def api(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=900) as r:
        raw = r.read()
        return json.loads(raw) if raw else {}


def demux(raw):
    out = []
    i = 0
    while i + 8 <= len(raw):
        size = int.from_bytes(raw[i + 4:i + 8], "big")
        out.append(raw[i + 8:i + 8 + size])
        i += 8 + size
    return b"".join(out).decode("utf-8", "ignore")


def main():
    name = sys.argv[1]
    cmd = sys.argv[2]
    ex = api("POST", f"/containers/{name}/exec",
             {"Cmd": ["sh", "-c", cmd], "AttachStdout": True, "AttachStderr": True, "Tty": False})
    eid = ex["Id"]
    req = urllib.request.Request(BASE + f"/exec/{eid}/start", data=json.dumps({"Detach": False, "Tty": False}).encode(),
                                 method="POST")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=900) as r:
        raw = r.read()
    info = api("GET", f"/exec/{eid}/json")
    sys.stdout.write(demux(raw))
    sys.stdout.write(f"\n[exit code: {info.get('ExitCode')}]\n")


if __name__ == "__main__":
    main()
