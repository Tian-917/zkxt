# -*- coding: utf-8 -*-
"""审计：ES 索引与 DB 上架课程的差异；并试探 data 服务接口是否可用"""
import io
import json
import subprocess
import urllib.request

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"


def sql(q):
    r = subprocess.run([MYSQL, "-h192.168.150.101", "-uroot", "-p123", "-D", "zk_course",
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", q],
                       capture_output=True)
    return r.stdout.decode("utf-8", "ignore")


es = json.loads(urllib.request.urlopen(
    "http://192.168.150.101:9200/course/_search?size=50&_source=id,name", timeout=10).read())
es_ids = {str(h["_source"]["id"]) for h in es["hits"]["hits"]}

rows = [l.split("\t") for l in sql("select id,name from course where status=2").splitlines() if l]
print("ES 文档 %d 条 / DB 上架 %d 门" % (len(es_ids), len(rows)))
missing = [(cid, name) for cid, name in rows if cid not in es_ids]
for cid, name in missing:
    print("  缺索引:", cid, name)
if not missing:
    print("  索引齐全")

print("\n== 试探 data-service 接口 ==")
for path in ["/ds/board?type=1", "/ds/today"]:
    try:
        with urllib.request.urlopen("http://localhost:8093" + path, timeout=10) as r:
            print(path, "->", r.status, r.read(200).decode("utf-8", "ignore"))
    except urllib.error.HTTPError as e:
        print(path, "-> HTTP", e.code, e.read(200).decode("utf-8", "ignore"))
    except Exception as e:
        print(path, "-> ERR", e)
