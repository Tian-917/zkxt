# -*- coding: utf-8 -*-
"""证书域并发压测

两组：
  A. 幂等：同一场考试 500 并发 -> 只应产生 1 张证
  B. 唯一：300 个不同考试并发 -> 300 张证，编号不重复

输出：成功率、错误分布、P50/P95/P99、编号冲突情况
"""
import concurrent.futures
import json
import subprocess
import time
import urllib.error
import urllib.request

URL = "http://localhost:8091/cert/issue"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"


def sql(q):
    r = subprocess.run([MYSQL, "-h192.168.150.101", "-uroot", "-p123", "-D", "zk_cert",
                        "--default-character-set=utf8mb4", "--batch", "--raw", "-N", "-e", q],
                       capture_output=True)
    return [l.split("\t") for l in r.stdout.decode("utf-8", "ignore").splitlines() if l]


def post(payload):
    t0 = time.perf_counter()
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(URL, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            body = json.loads(r.read().decode("utf-8"))
            return time.perf_counter() - t0, body.get("certNo"), None
    except urllib.error.HTTPError as e:
        return time.perf_counter() - t0, None, "HTTP %d" % e.code
    except Exception as e:
        return time.perf_counter() - t0, None, type(e).__name__


def percentile(vals, p):
    if not vals:
        return 0.0
    s = sorted(vals)
    k = max(0, min(len(s) - 1, int(round((p / 100.0) * (len(s) - 1)))))
    return s[k]


def run(name, payloads, workers):
    print("\n=== %s：%d 个请求 / %d 并发 ===" % (name, len(payloads), workers))
    t0 = time.perf_counter()
    results = list(concurrent.futures.ThreadPoolExecutor(max_workers=workers).map(post, payloads))
    wall = time.perf_counter() - t0

    lat = [r[0] for r in results]
    oks = [r for r in results if r[2] is None]
    errs = {}
    for r in results:
        if r[2]:
            errs[r[2]] = errs.get(r[2], 0) + 1
    certs = [r[1] for r in oks]

    print("耗时 %.2fs  QPS %.0f  成功 %d/%d" % (wall, len(payloads) / wall, len(oks), len(payloads)))
    print("延迟 P50 %.0fms  P95 %.0fms  P99 %.0fms  MAX %.0fms"
          % (percentile(lat, 50) * 1000, percentile(lat, 95) * 1000,
             percentile(lat, 99) * 1000, max(lat) * 1000))
    if errs:
        print("错误分布:", errs)
    return certs, errs


# A. 幂等
payload_a = {"userId": 1, "userName": "林晓", "certType": "并发压测",
             "examId": 900001, "score": 88, "studyMinutes": 200}
certs_a, errs_a = run("A 幂等（同一考试 500 并发）", [payload_a] * 500, 100)
print("返回的不同证书编号数: %d（期望 1）" % len(set(certs_a)))

# B. 编号唯一
payloads_b = [{"userId": 1, "userName": "林晓", "certType": "并发压测",
               "examId": 900100 + i, "score": 90, "studyMinutes": 200} for i in range(300)]
certs_b, errs_b = run("B 编号唯一（300 个不同考试并发）", payloads_b, 100)
print("返回证书数: %d，去重后: %d（期望相等）" % (len(certs_b), len(set(certs_b))))

# 数据库校验
print("\n=== 数据库校验 ===")
rows = sql("SELECT COUNT(*) FROM certificate WHERE exam_id = 900001")
print("exam_id=900001 的证书行数: %s（期望 1）" % rows[0][0])
rows = sql("SELECT COUNT(*), COUNT(DISTINCT cert_no) FROM certificate")
print("证书总数 %s，编号去重后 %s（期望相等）" % (rows[0][0], rows[0][1]))
rows = sql("SELECT COUNT(*) FROM certificate WHERE exam_id BETWEEN 900100 AND 900399")
print("B 组落库行数: %s（期望 300）" % rows[0][0])
