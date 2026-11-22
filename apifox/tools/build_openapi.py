# -*- coding: utf-8 -*-
"""Convert cached Apifox shared-docs data into OpenAPI 3.0 documents."""
import json
import os
import re
import sys

CACHE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".apifox-cache")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "tianji-openapi.json")
OUT_DUP = os.path.join(ROOT, "tianji-openapi-dup.json")

TYPE_MAP = {
    "integer": "integer", "long": "integer", "number": "number", "string": "string",
    "boolean": "boolean", "array": "array", "object": "object", "file": "string",
}


def load(name):
    with open(os.path.join(CACHE, name), encoding="utf-8") as f:
        return json.load(f)


def safe_name(raw, fallback):
    name = (raw or "").strip()
    name = re.sub(r"[^A-Za-z0-9_.\-]", "_", name)
    name = re.sub(r"_{2,}", "_", name).strip("_")
    if not name or name[0].isdigit():
        name = (fallback + "_" + name) if name else fallback
    return name


def build_schema_index(schemas):
    by_id = {}
    used = set()
    for s in schemas:
        raw = s.get("displayName") or s.get("name") or ""
        base = safe_name(raw, "Schema")
        name, i = base, 2
        while name.lower() in used:
            name = "%s_%d" % (base, i)
            i += 1
        used.add(name.lower())
        by_id[s["id"]] = (name, s)
    return by_id


REF_RE = re.compile(r"^#/definitions/(\d+)$")


def rewrite(node, index, out_components, seen):
    if isinstance(node, dict):
        if set(node.keys()) == {"$ref"}:
            m = REF_RE.match(node["$ref"])
            if m and int(m.group(1)) in index:
                name, s = index[int(m.group(1))]
                out_components[name] = finalize_schema(s, index, out_components, seen, name)
                return {"$ref": "#/components/schemas/%s" % name}
            return dict(node)
        out = {}
        for k, v in node.items():
            if k == "$ref" and isinstance(v, str):
                m = REF_RE.match(v)
                if m and int(m.group(1)) in index:
                    name, s = index[int(m.group(1))]
                    out_components[name] = finalize_schema(s, index, out_components, seen, name)
                    out[k] = "#/components/schemas/%s" % name
                else:
                    out[k] = v
            else:
                out[k] = rewrite(v, index, out_components, seen)
        return out
    if isinstance(node, list):
        return [rewrite(v, index, out_components, seen) for v in node]
    return node


def finalize_schema(entry, index, out_components, seen, name):
    if name in seen:
        return {"$ref": "#/components/schemas/%s" % name}
    seen.add(name)
    schema = rewrite(entry.get("jsonSchema") or {}, index, out_components, seen)
    if entry.get("description") and "description" not in schema:
        schema["description"] = entry["description"]
    if entry.get("displayName") or entry.get("name"):
        schema.setdefault("title", entry.get("displayName") or entry.get("name"))
    return schema


def param_schema(p):
    t = TYPE_MAP.get((p.get("type") or "string").lower(), "string")
    schema = {"type": t}
    if t == "array":
        schema["items"] = {"type": "string"}
    if p.get("description"):
        schema["description"] = p["description"]
    sv = p.get("sampleValue")
    if sv not in (None, ""):
        try:
            if t == "integer":
                schema["example"] = int(sv)
            elif t == "number":
                schema["example"] = float(sv)
            elif t == "boolean":
                schema["example"] = str(sv).lower() == "true"
            else:
                schema["example"] = sv
        except (TypeError, ValueError):
            pass
    return schema


def make_operation(detail, folder_path, index, components, aid):
    op = {
        "summary": detail.get("name") or "",
        "operationId": detail.get("operationId") or ("op_%d" % aid),
        "tags": [folder_path[0]] if folder_path else [],
    }
    folder = "/".join(folder_path)
    if folder:
        op["x-apifox-folder"] = folder
    if detail.get("description"):
        op["description"] = detail["description"]

    params = []
    for loc in ("path", "query", "header", "cookie"):
        for p in (detail.get("parameters", {}).get(loc) or []):
            if not p.get("name"):
                continue
            params.append({
                "name": p["name"],
                "in": loc,
                "required": bool(p.get("required")) or loc == "path",
                "schema": param_schema(p),
            })
    if params:
        op["parameters"] = params

    rb = detail.get("requestBody") or {}
    rb_type = rb.get("type") or "none"
    if rb_type not in ("none", "") and rb.get("jsonSchema"):
        ct = rb_type if "/" in rb_type else "application/json"
        op["requestBody"] = {
            "required": True,
            "content": {ct: {"schema": rewrite(rb["jsonSchema"], index, components, set())}},
        }

    responses = {}
    for r in (detail.get("responses") or []):
        code = str(r.get("code") or "200")
        body = {"description": r.get("description") or r.get("name") or ""}
        js = r.get("jsonSchema")
        if js and (js.get("type") or js.get("$ref") or js.get("properties") is not None):
            ctype = "application/json"
            ct_field = (r.get("contentType") or "json").lower()
            if ct_field == "xml":
                ctype = "application/xml"
            elif ct_field in ("text", "html"):
                ctype = "text/plain"
            body["content"] = {ctype: {"schema": rewrite(js, index, components, set())}}
        responses[code] = body
    if not responses:
        responses["200"] = {"description": "OK"}
    op["responses"] = responses
    return op


def main():
    tree = load("tree.json")
    schemas = load("schemas.json")
    index = build_schema_index(schemas)

    paths, dup_paths = {}, {}
    components = {}
    operations = skipped = 0
    folders = []

    def walk(nodes, folder_path):
        nonlocal operations, skipped
        for n in nodes:
            if n.get("type") == "apiDetailFolder":
                walk(n.get("children") or [], folder_path + [n["name"]])
            elif n.get("type") == "apiDetail":
                aid = n["api"]["id"]
                detail = load("api-%d.json" % aid)
                path = detail.get("path") or ""
                method = (detail.get("method") or "get").lower()
                if not path.startswith("/"):
                    path = "/" + path
                folder = "/".join(folder_path)
                if folder and folder not in folders:
                    folders.append(folder)
                op = make_operation(detail, folder_path, index, components, aid)
                target = paths
                if path in paths and method in paths[path]:
                    target = dup_paths
                    skipped += 1
                target.setdefault(path, {})[method] = op
                operations += 1

    walk(tree, [])

    base = {
        "openapi": "3.0.1",
        "info": {
            "title": "天机学堂项目",
            "description": "由 Apifox 分享文档 https://s.apifox.cn/c942b97d-afec-4888-b38a-7e211989483f 转换",
            "version": "1.0.0",
        },
    }
    doc = dict(base, paths=paths, components={"schemas": components})
    dup_doc = dict(base, paths=dup_paths, components={"schemas": components})

    for path, d in ((OUT, doc), (OUT_DUP, dup_doc)):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(d, f, ensure_ascii=False, indent=1)

    print("operations:", operations, " unique-paths:", len(paths), " duplicates:", skipped)
    print("schemas:", len(components), " folders:", len(folders))
    print("main:", round(os.path.getsize(OUT) / 1024, 1), "KB")


if __name__ == "__main__":
    sys.exit(main())
