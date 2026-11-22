# -*- coding: utf-8 -*-
"""清理 VM 上本次 CI 部署的 zkxt-* 容器。用法: python tools/vm_rm_zkxt.py"""
import json
import urllib.request

BASE = "http://192.168.150.101:2375"


def api(method, path):
    req = urllib.request.Request(BASE + path, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status
    except urllib.error.HTTPError as e:
        return e.code


def main():
    with urllib.request.urlopen(BASE + "/containers/json?all=1", timeout=30) as r:
        containers = json.load(r)
    targets = [c["Names"][0].lstrip("/") for c in containers if c["Names"][0].startswith("/zkxt-")]
    if not targets:
        print("没有 zkxt-* 容器")
        return
    print("待清理:", targets)
    for name in targets:
        s = api("POST", f"/containers/{name}/stop?t=5")
        d = api("DELETE", f"/containers/{name}?force=true")
        print(f"  {name}: stop={s} rm={d}")
    with urllib.request.urlopen(BASE + "/containers/json?all=1", timeout=30) as r:
        left = [c["Names"][0] for c in json.load(r) if c["Names"][0].startswith("/zkxt-")]
    print("剩余:", left if left else "无")


if __name__ == "__main__":
    main()
