# -*- coding: utf-8 -*-
"""Fetch the public shared-docs data for an Apifox share link."""
import json
import os
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

SID = "c942b97d-afec-4888-b38a-7e211989483f"
BASE = "https://api.apifox.cn/api/v1/shared-docs/" + SID
CACHE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".apifox-cache")
HEADERS = {
    "Accept": "application/json, text/plain, */*",
    "Origin": "https://s.apifox.cn",
    "Referer": "https://s.apifox.cn/" + SID,
    "X-Apifox-Api-Version": "2024-03-28",
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/131.0 Safari/537.36",
}


def get(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.loads(resp.read().decode("utf-8"))


def main():
    os.makedirs(CACHE, exist_ok=True)

    tree = get(BASE + "/http-api-tree")["data"]
    with open(os.path.join(CACHE, "tree.json"), "w", encoding="utf-8") as f:
        json.dump(tree, f, ensure_ascii=False, indent=1)

    schemas = get(BASE + "/data-schemas")["data"]
    with open(os.path.join(CACHE, "schemas.json"), "w", encoding="utf-8") as f:
        json.dump(schemas, f, ensure_ascii=False, indent=1)

    api_ids = []

    def walk(nodes):
        for n in nodes:
            if n.get("type") == "apiDetail":
                api_ids.append(n["api"]["id"])
            walk(n.get("children") or [])

    walk(tree)
    print("folders/apis discovered, api count =", len(api_ids))

    def fetch_one(aid):
        path = os.path.join(CACHE, "api-%d.json" % aid)
        if os.path.exists(path):
            return aid, "cached"
        data = get(BASE + "/http-apis/%d" % aid)["data"]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=1)
        return aid, "ok"

    ok = fail = 0
    failures = []
    with ThreadPoolExecutor(max_workers=8) as pool:
        for aid, status in pool.map(fetch_one, api_ids):
            if status == "ok":
                ok += 1
            elif status == "cached":
                ok += 1
            else:
                fail += 1
            if status not in ("ok", "cached"):
                failures.append(aid)

    print("fetched:", ok, "failed:", fail, failures[:10])
    with open(os.path.join(CACHE, "_api_ids.json"), "w", encoding="utf-8") as f:
        json.dump(api_ids, f)


if __name__ == "__main__":
    sys.exit(main())
