# -*- coding: utf-8 -*-
"""刷新 data-service 看板的「热门课程 TOP10」快照。

原来这两个 key 是课程环境的历史快照（课程名对不上我们自己的 14 门课）。
这里按 data-service 读的结构（CourseInfo: category/name/newStuNum/orderAmount）
用我们自己的数据重写一遍，等价于跑了一次聚合任务。

key 规则：DATA:TOP10:{version}，version = 当月第几天 % 1 = 0
"""
import json
import socket
import subprocess

MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
REDIS = ("192.168.150.101", 6379)
PASSWORD = "123321"


def sql(q):
    r = subprocess.run(
        [MYSQL, "-h192.168.150.101", "-uroot", "-p123", "-D", "zk_course",
         "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", q],
        capture_output=True)
    return r.stdout.decode("utf-8", "ignore")


def resp(*args):
    out = b"*%d\r\n" % len(args)
    for a in args:
        b = str(a).encode("utf-8")
        out += b"$%d\r\n%s\r\n" % (len(b), b)
    return out


# 课程 + 报名人数（learning_lesson 计数）
rows = sql("""
SELECT c.id, c.name, c.price,
       (SELECT COUNT(*) FROM zk_learning.learning_lesson l WHERE l.course_id = c.id) AS stu,
       (SELECT COUNT(*) FROM zk_learning.learning_lesson l WHERE l.course_id = c.id) * c.price / 100 AS amount
FROM course c
WHERE c.status = 2
ORDER BY stu DESC, c.id
""")

data = []
for line in rows.splitlines():
    parts = line.split("\t")
    if len(parts) != 5:
        continue
    cid, name, price, stu, amount = parts
    data.append({
        "category": "职业技能考证",
        "name": name,
        "newStuNum": int(float(stu or 0)),
        "orderAmount": round(float(amount or 0), 2),
    })

payload = json.dumps(data, ensure_ascii=False)
print("课程数:", len(data))
print("示例:", json.dumps(data[:2], ensure_ascii=False))

s = socket.create_connection(REDIS, timeout=8)
s.sendall(resp("AUTH", PASSWORD)); s.recv(1024)
s.sendall(resp("SET", "DATA:TOP10:0", payload)); print("SET DATA:TOP10:0 ->", s.recv(1024).decode().strip())
s.sendall(resp("GET", "DATA:TOP10:0"))
back = s.recv(65536).decode("utf-8", "ignore")
print("回读长度:", len(back))
s.close()
