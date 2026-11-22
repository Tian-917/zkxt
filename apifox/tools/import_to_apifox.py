# -*- coding: utf-8 -*-
"""Import the generated OpenAPI document into a local Apifox project."""
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
DOC = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "tianji-openapi.json")


def call(method, url, payload=None):
    data = json.dumps(payload).encode("utf-8") if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers={
        "Authorization": TOKEN,
        "Content-Type": "application/json",
        "Accept": "application/json",
        "X-Apifox-Api-Version": "2024-03-28",
        "User-Agent": "Mozilla/5.0",
    })
    try:
        with urllib.request.urlopen(req, timeout=600) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(body)
        except ValueError:
            return e.code, body[:800]


def main():
    with open(DOC, encoding="utf-8") as f:
        doc = json.load(f)
    doc_str = json.dumps(doc, ensure_ascii=False)

    payload = {
        "input": doc_str,
        "options": {
            "endpointOverwriteBehavior": "OVERWRITE_EXISTING",
            "schemaOverwriteBehavior": "OVERWRITE_EXISTING",
            "updateFolderOfChangedEndpoint": True,
            "prependBasePath": False,
        },
    }
    url = "%s/projects/%d/import-openapi" % (BASE, PROJECT_ID)
    status, resp = call("POST", url, payload)
    print("import status:", status)
    print(json.dumps(resp, ensure_ascii=False)[:1500])

    status, tree = call("GET", "%s/projects/%d/http-api-tree" % (BASE, PROJECT_ID))
    print("tree status:", status)
    if isinstance(tree, dict) and tree.get("data") is not None:
        counts = {"folder": 0, "api": 0}

        def walk(nodes):
            for n in nodes:
                if n.get("type") == "apiDetailFolder":
                    counts["folder"] += 1
                    walk(n.get("children") or [])
                elif n.get("type") == "apiDetail":
                    counts["api"] += 1

        walk(tree["data"])
        print("project tree now:", counts)
        print("top-level folders:", [n.get("name") for n in tree["data"] if n.get("type") == "apiDetailFolder"][:15])


if __name__ == "__main__":
    sys.exit(main())
