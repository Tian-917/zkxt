# -*- coding: utf-8 -*-
"""Import the duplicate-path operations with CREATE_NEW, then verify the project."""
import json
import os
import sys
import urllib.error
import urllib.request

PROJECT_ID = 8944308
TOKEN = os.environ.get("APIFOX_TOKEN", "")
if not TOKEN:
    sys.exit("Set APIFOX_TOKEN (e.g. 'Bearer <access token>') before running this script.")
BASE = "https://api.apifox.com/api/v1"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DUP = os.path.join(ROOT, "tianji-openapi-dup.json")


def call(method, url, payload=None):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers={
        "Authorization": TOKEN, "Content-Type": "application/json",
        "Accept": "application/json", "X-Apifox-Api-Version": "2024-03-28",
        "User-Agent": "Mozilla/5.0",
    })
    try:
        with urllib.request.urlopen(req, timeout=600) as r:
            raw = r.read().decode("utf-8")
            return r.status, (json.loads(raw) if raw else {})
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(body)
        except ValueError:
            return e.code, body[:500]


def list_all_apis():
    items, page = [], 1
    while True:
        s, resp = call("GET", "%s/projects/%d/http-apis?page=%d&pageSize=100" % (BASE, PROJECT_ID, page))
        if s != 200:
            print("list failed", s, resp)
            break
        data = resp.get("data") or []
        items.extend(data)
        meta = resp.get("meta") or {}
        total = meta.get("total") or meta.get("pagination", {}).get("total")
        if not data or (total and len(items) >= total) or len(data) < 100:
            break
        page += 1
    return items


def main():
    with open(DUP, encoding="utf-8") as f:
        doc = json.load(f)
    payload = {
        "input": json.dumps(doc, ensure_ascii=False),
        "options": {
            "endpointOverwriteBehavior": "CREATE_NEW",
            "schemaOverwriteBehavior": "KEEP_EXISTING",
            "updateFolderOfChangedEndpoint": True,
            "prependBasePath": False,
        },
    }
    s, resp = call("POST", "%s/projects/%d/import-openapi" % (BASE, PROJECT_ID), payload)
    print("dup import:", s, json.dumps(resp.get("data", {}).get("counters", resp), ensure_ascii=False)[:600])

    apis = list_all_apis()
    print("total endpoints:", len(apis))
    from collections import Counter
    dup = Counter((a["path"], a["method"]) for a in apis)
    print("path+method still duplicated:", {k: v for k, v in dup.items() if v > 1})

    sample = [a for a in apis if a["path"] == "/courses/page"]
    if sample:
        s2, full = call("GET", "%s/projects/%d/http-apis/%d" % (BASE, PROJECT_ID, sample[0]["id"]))
        d = full.get("data", {})
        print("sample:", d.get("name"), d.get("method"), d.get("path"), "folder:", d.get("folderId"))
        print("  query params:", [p["name"] for p in (d.get("parameters", {}).get("query") or [])])
        print("  responses:", [(r["code"], str(r.get("jsonSchema"))[:40]) for r in (d.get("responses") or [])][:3])


if __name__ == "__main__":
    sys.exit(main())
