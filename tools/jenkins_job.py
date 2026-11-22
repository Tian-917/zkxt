# -*- coding: utf-8 -*-
"""在 Jenkins 上创建/更新 pipeline 任务并触发构建。

用法:
  python tools/jenkins_job.py create   # 创建（存在则更新）
  python tools/jenkins_job.py build    # 触发并等待，打印日志尾巴
  python tools/jenkins_job.py status
"""
import base64
import http.cookiejar
import json
import sys
import time
import urllib.error
import urllib.request

BASE = "http://192.168.150.101:18080"
USER, PWD = "root", "Jenkins"
JOB = "zkxt-pipeline"
GIT_URL = "http://192.168.150.101:10880/zkxt/zkxt.git"

CONFIG = """<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job">
  <description>职考云：Gogs 拉代码 → Maven 打包 → 构建镜像 → 部署到虚拟机 Docker → 冒烟</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps">
    <scm class="hudson.plugins.git.GitSCM" plugin="git">
      <configVersion>2</configVersion>
      <userRemoteConfigs>
        <hudson.plugins.git.UserRemoteConfig>
          <url>%s</url>
        </hudson.plugins.git.UserRemoteConfig>
      </userRemoteConfigs>
      <branches>
        <hudson.plugins.git.BranchSpec>
          <name>*/main</name>
        </hudson.plugins.git.BranchSpec>
      </branches>
      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
      <submoduleCfg class="empty-list"/>
      <extensions/>
    </scm>
    <scriptPath>Jenkinsfile</scriptPath>
    <lightweight>true</lightweight>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
""" % GIT_URL

_CJ = http.cookiejar.CookieJar()
_OPENER = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(_CJ))
_AUTH = "Basic " + base64.b64encode(f"{USER}:{PWD}".encode()).decode()


def _request(method, path, data=None, ctype=None):
    r = urllib.request.Request(BASE + path, data=data, method=method)
    r.add_header("Authorization", _AUTH)
    if ctype:
        r.add_header("Content-Type", ctype)
    try:
        with _OPENER.open(r, timeout=120) as resp:
            return resp.status, resp.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def _crumb():
    code, body = _request("GET", "/crumbIssuer/api/json")
    if code != 200:
        return None, None
    d = json.loads(body)
    return d["crumbRequestField"], d["crumb"]


def post(path, data=None, ctype="application/xml"):
    field, value = _crumb()
    r = urllib.request.Request(BASE + path, data=data, method="POST")
    r.add_header("Authorization", _AUTH)
    if ctype:
        r.add_header("Content-Type", ctype)
    if field and value:
        r.add_header(field, value)
    try:
        with _OPENER.open(r, timeout=120) as resp:
            return resp.status, resp.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def create():
    code, body = post(f"/createItem?name={JOB}", CONFIG.encode(), "application/xml")
    if code == 200:
        print("created job:", JOB)
        return True
    if code == 400 and "already exists" in body:
        code2, _ = post(f"/job/{JOB}/config.xml", CONFIG.encode(), "application/xml")
        print("updated job:", JOB, code2)
        return code2 == 200
    print("create failed:", code, body[:300])
    return False


def build_and_wait(max_wait=1800):
    # 该 job 声明了参数，必须走 buildWithParameters 并带上表单数据
    code, body = post(f"/job/{JOB}/buildWithParameters", b'SERVICES=&SKIP_TESTS=true&delay=0sec', 'application/x-www-form-urlencoded')
    print("trigger:", code)
    if code not in (200, 201):
        print(body[:300])
        return False
    time.sleep(6)
    deadline = time.time() + max_wait
    while time.time() < deadline:
        code, body = _request("GET", f"/job/{JOB}/lastBuild/api/json?tree=number,building,result")
        if code == 200:
            d = json.loads(body)
            if not d.get("building"):
                print("build #%s result=%s" % (d.get("number"), d.get("result")))
                _, log = _request("GET", f"/job/{JOB}/lastBuild/consoleText")
                print("\n".join(log.splitlines()[-70:]))
                return d.get("result") == "SUCCESS"
        time.sleep(10)
    print("timeout")
    return False


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "status"
    if cmd == "create":
        create()
    elif cmd == "build":
        ok = build_and_wait()
        sys.exit(0 if ok else 1)
    else:
        print(_request("GET", f"/job/{JOB}/lastBuild/api/json?tree=number,building,result"))
