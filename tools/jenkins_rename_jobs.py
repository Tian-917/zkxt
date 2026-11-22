# -*- coding: utf-8 -*-
"""把 Jenkins 里 tj-* / tjxt-* 的旧 job 改名为 zkxt-*，并清理描述里的天机学堂字样。"""
import json
import re
import sys
import urllib.parse

sys.path.insert(0, "tools")
import jenkins_job as J  # noqa: E402


def list_jobs():
    code, body = J._request("GET", "/api/json?tree=jobs[name]")
    assert code == 200, body[:200]
    return [j["name"] for j in json.loads(body)["jobs"]]


def rename(old, new):
    q = urllib.parse.quote(old, safe="")
    code, body = J.post(f"/job/{q}/doRename?newName={urllib.parse.quote(new, safe='')}",
                        b"", "application/x-www-form-urlencoded")
    return code, body


def clean_desc(name):
    q = urllib.parse.quote(name, safe="")
    code, cfg = J._request("GET", f"/job/{q}/config.xml")
    if code != 200:
        return code
    new = cfg
    for a, b in [("天机学堂", "职考云"), ("tjxt", "zkxt"), ("tj-", "zkxt-")]:
        new = new.replace(a, b)
    if new == cfg:
        return 200
    code2, _ = J.post(f"/job/{q}/config.xml", new.encode("utf-8"), "application/xml")
    return code2


def main():
    jobs = list_jobs()
    targets = [n for n in jobs if n.startswith("tj-") or n.startswith("tjxt")]
    print("待改名:", len(targets))
    ok = 0
    for old in targets:
        new = old.replace("tjxt", "zkxt").replace("tj-", "zkxt-")
        code, _ = rename(old, new)
        if code in (200, 302):
            c2 = clean_desc(new)
            print(f"  {old} -> {new}  (desc {c2})")
            ok += 1
        else:
            print(f"  !! {old} -> {new} 失败 code={code}")
    print(f"完成 {ok}/{len(targets)}")
    print("现在的 job:", list_jobs())


if __name__ == "__main__":
    main()
