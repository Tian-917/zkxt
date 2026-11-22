# -*- coding: utf-8 -*-
"""列出 zk_user / zk_learning 的真实表名并统计行数。"""
import subprocess

M = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"


def q(sql):
    r = subprocess.run([M, "-h", "192.168.150.101", "-uroot", "-p123", "-N", "-B",
                        "--default-character-set=utf8mb4", "-e", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="ignore")
    return (r.stdout or r.stderr).strip()


for schema in ("zk_user", "zk_learning"):
    print("== %s" % schema)
    tables = q("select table_name from information_schema.tables where table_schema='%s' order by table_name" % schema)
    for t in tables.splitlines():
        t = t.strip()
        if not t:
            continue
        n = q("select count(*) from %s.%s" % (schema, t))
        print("   %-32s %s" % (t, n))
