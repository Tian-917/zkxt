# -*- coding: utf-8 -*-
"""把 zk_course 里的考证课包同步到 ES 的 course 索引。

换库改数据后 ES 还是旧课程名，搜索页会露馅。正式链路是 course-service 上架时发
MQ(course.up) -> search-service 重建文档；这里为了不启全量服务，直接用 bulk 更新。
"""
import json
import subprocess
import urllib.parse
import urllib.request

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
ES = "http://192.168.150.101:9200"
HOST, USER, PWD = "192.168.150.101", "root", "123"


def rows(sql):
    r = subprocess.run([MYSQL, "-h" + HOST, "-u" + USER, "-p" + PWD, "-D", "zk_course",
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", sql],
                       capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode("utf-8", "ignore"))
    return [l.split("\t") for l in r.stdout.decode("utf-8", "ignore").splitlines() if l]


def es(path, body=None, method="GET"):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(ES + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, timeout=10) as r:
        return json.loads(r.read().decode("utf-8"))


courses = rows(
    "select c.id, c.name, c.price, c.first_cate_id, c.second_cate_id, c.free, ct.teacher_id "
    "from course c "
    "left join course_teacher ct on ct.course_id = c.id")
by_id = {c[0]: c for c in courses}

res = es("/course/_search?size=200&_source=id,name,price,categoryIdLv1,categoryIdLv2")
hits = res["hits"]["hits"]
lines, changed = [], 0
for h in hits:
    did = str(h["_source"].get("id", h["_id"]))
    c = by_id.get(did)
    if not c:
        continue
    cid, name, price, cate1, cate2, free, teacher = c
    lines.append(json.dumps({"update": {"_index": "course", "_id": h["_id"]}}, ensure_ascii=False))
    lines.append(json.dumps({"doc": {
        "name": name,
        "price": int(price),
        "categoryIdLv1": int(cate1),
        "categoryIdLv2": int(cate2),
        "free": free == "1",
        "teacher": int(teacher) if teacher and teacher.isdigit() else None,
    }}, ensure_ascii=False))
    changed += 1

if changed:
    body = "\n".join(lines) + "\n"
    req = urllib.request.Request(ES + "/_bulk", data=body.encode("utf-8"), method="POST")
    req.add_header("Content-Type", "application/x-ndjson")
    with urllib.request.urlopen(req, timeout=15) as r:
        out = json.loads(r.read().decode("utf-8"))
    print("ES 文档更新: %d 条, errors=%s" % (changed, out.get("errors")))

print("校验(搜索 会计):")
res = es("/course/_search?size=3&q=" + urllib.parse.quote("会计"))
for h in res["hits"]["hits"]:
    print("  ", h["_source"].get("name"), "| price=", h["_source"].get("price"),
          "| cate=", h["_source"].get("categoryIdLv1"), h["_source"].get("categoryIdLv2"))
