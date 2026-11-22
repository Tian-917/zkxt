# -*- coding: utf-8 -*-
"""重新拆分历史：严格遵循 .gitignore，绝不包含 target 等构建产物。"""
import os
import subprocess
import sys

GROUPS = [
    ("搭建项目骨架与公共模块", [".gitignore", "pom.xml", "zk-common", "zk-api"]),
    ("接入网关并实现统一鉴权", ["zk-gateway", "zk-auth"]),
    ("实现用户与权限管理", ["zk-user"]),
    ("实现课程与目录编排", ["zk-course"]),
    ("实现学习进度、笔记与问答", ["zk-learning"]),
    ("实现题库、考试与错题本", ["zk-exam"]),
    ("实现证书签发与验真", ["zk-cert"]),
    ("实现订单、退款与支付", ["zk-trade", "zk-pay"]),
    ("实现优惠券与兑换码", ["zk-promotion"]),
    ("接入搜索、媒资、消息与点赞", ["zk-search", "zk-media", "zk-message", "zk-remark"]),
    ("实现数据看板统计", ["zk-data"]),
    ("完成学员端与管理端前端", ["web"]),
    ("补充部署脚本、接口文档与项目说明", ["docker", "docs", "Jenkinsfile", "apifox", "tools", "README.md"]),
]

IDENT = ["-c", "user.name=Tian00001", "-c", "user.email=2904221032@qq.com"]


def run(args, check=True):
    r = subprocess.run(args, capture_output=True, text=True, encoding="utf-8", errors="ignore")
    if check and r.returncode != 0:
        print("FAILED:", " ".join(args))
        print(r.stdout[-400:], r.stderr[-400:])
        sys.exit(1)
    return r


run(["git", "checkout", "-q", "--orphan", "hist2"])
run(["git", "rm", "-r", "--cached", ".", "-q"], check=False)

for msg, paths in GROUPS:
    existing = [p for p in paths if os.path.exists(p)]
    if not existing:
        print("跳过:", msg)
        continue
    run(["git", "add"] + existing)          # 不加 -f，遵循 .gitignore
    r = run(["git"] + IDENT + ["commit", "-q", "-m", msg], check=False)
    print(("提交: " if r.returncode == 0 else "无变更: ") + msg)

run(["git", "add", "-A"])
r = run(["git"] + IDENT + ["commit", "-q", "-m", "补充遗漏文件"], check=False)

n = run(["git", "rev-list", "--count", "HEAD"]).stdout.strip()
print("提交总数:", n)

# 安全检查：树里不能有构建产物
tree = run(["git", "ls-tree", "-r", "--name-only", "HEAD"]).stdout.splitlines()
bad = [f for f in tree if "/target/" in f or f.endswith(".jar") or "node_modules" in f]
big = run(["git", "ls-tree", "-r", "-l", "HEAD"]).stdout.splitlines()
huge = [l for l in big if int(l.split()[3]) > 50 * 1024 * 1024]
print("构建产物文件数:", len(bad), "| 超过 50MB 的文件:", len(huge))
print("文件总数:", len(tree))
