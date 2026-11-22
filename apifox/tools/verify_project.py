# -*- coding: utf-8 -*-
"""Verify imported endpoints and data schemas in the Apifox project."""
import json
import sys
import urllib.request
from collections import Counter

import os

TOKEN = os.environ.get("APIFOX_TOKEN", "")
if not TOKEN:
    sys.exit("Set APIFOX_TOKEN (e.g. 'Bearer <access token>') before running this script.")
BASE = "https://api.apifox.com/api/v1"
PID = 8944308


def get(url):
    req = urllib.request.Request(url, headers={
        "Authorization": TOKEN, "Accept": "application/json",
        "X-Apifox-Api-Version": "2024-03-28", "User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=90) as r:
        raw = r.read().decode("utf-8")
        return json.loads(raw) if raw else {}


def main():
    apis, page = [], 1
    while True:
        resp = get("%s/projects/%d/http-apis?page=%d&perPage=100" % (BASE, PID, page))
        apis.extend(resp.get("data") or [])
        meta = resp.get("meta") or {}
        if page >= (meta.get("totalPages") or 1):
            break
        page += 1

    print("endpoints:", len(apis), "| meta total:", meta.get("total"))
    dup = {k: v for k, v in Counter((a["path"], a["method"]) for a in apis).items() if v > 1}
    print("duplicate path+method:", dup or "none")
    print("methods:", dict(Counter(a["method"] for a in apis)))

    folders = {f["id"]: f["name"] for f in get("%s/projects/%d/api-folders" % (BASE, PID))["data"]}
    by_folder = Counter(folders.get(a.get("folderId"), "?") for a in apis)
    print("top folders:", len(by_folder))
    for name, cnt in by_folder.most_common():
        print("   %-28s %d" % (name, cnt))

    schemas = get("%s/projects/%d/data-schemas?page=1&perPage=100" % (BASE, PID))
    print("data schemas:", (schemas.get("meta") or {}).get("total"), "returned:", len(schemas.get("data") or []))

    # content spot-check: an endpoint with json body
    target = next((a for a in apis if a["path"] == "/courses/upShelf"), None) or apis[0]
    full = get("%s/projects/%d/http-apis/%d" % (BASE, PID, target["id"]))["data"]
    print("\nspot-check:", full.get("name"), full.get("method").upper(), full.get("path"))
    print("  folder:", folders.get(full.get("folderId")))
    print("  query:", [p["name"] for p in (full.get("parameters", {}).get("query") or [])])
    rb = full.get("requestBody") or {}
    print("  body type:", rb.get("type"), "| schema ref:", str(rb.get("jsonSchema"))[:80])
    for r in (full.get("responses") or [])[:2]:
        print("  response:", r["code"], str(r.get("jsonSchema"))[:90])


if __name__ == "__main__":
    sys.exit(main())
