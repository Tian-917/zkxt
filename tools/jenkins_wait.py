# -*- coding: utf-8 -*-
"""等指定构建号结束并打印日志尾部。用法: python tools/jenkins_wait.py <buildNo> [tail]"""
import base64
import json
import sys
import time
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:18080"
AUTH = "Basic " + base64.b64encode(b"root:Jenkins").decode()
JOB = "zkxt-pipeline"


def get(path):
    r = urllib.request.Request(BASE + path)
    r.add_header("Authorization", AUTH)
    with urllib.request.urlopen(r, timeout=60) as resp:
        return resp.read().decode("utf-8", "ignore")


def main():
    no = int(sys.argv[1])
    tail = int(sys.argv[2]) if len(sys.argv) > 2 else 60
    deadline = time.time() + 3600
    while time.time() < deadline:
        d = json.loads(get(f"/job/{JOB}/{no}/api/json?tree=number,building,result"))
        if not d.get("building"):
            print(f"build #{d['number']} result={d['result']}")
            log = get(f"/job/{JOB}/{no}/consoleText")
            lines = log.splitlines()
            print("\n".join(lines[-tail:]))
            return 0 if d["result"] == "SUCCESS" else 1
        time.sleep(10)
    print("timeout")


if __name__ == "__main__":
    sys.exit(main() or 0)
