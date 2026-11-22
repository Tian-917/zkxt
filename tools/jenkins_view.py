# -*- coding: utf-8 -*-
"""在 Jenkins 里创建一个独立视图，把职考云的 job 和天机学堂的旧 job 分开。

用法: python tools/jenkins_view.py create
"""
import base64
import http.cookiejar
import json
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = "http://192.168.150.101:18080"
USER, PWD = "root", "Jenkins"
VIEW = "职考云"
JOBS = ["zkxt-pipeline"]

VIEW_XML = """<?xml version='1.1' encoding='UTF-8'?>
<hudson.model.ListView>
  <name>%s</name>
  <description>职考云 CI/CD：Gogs 拉代码 → Maven 打包 → Docker 构建部署到虚拟机</description>
  <filterExecutors>false</filterExecutors>
  <filterQueue>false</filterQueue>
  <properties class="hudson.model.View$PropertyList"/>
  <jobNames>
    <comparator class="java.lang.String$CaseInsensitiveComparator"/>
%s
  </jobNames>
  <jobFilters/>
  <columns>
    <hudson.views.StatusColumn/>
    <hudson.views.WeatherColumn/>
    <hudson.views.JobColumn/>
    <hudson.views.LastSuccessColumn/>
    <hudson.views.LastFailureColumn/>
    <hudson.views.LastDurationColumn/>
    <hudson.views.BuildButtonColumn/>
  </columns>
  <recurse>false</recurse>
</hudson.model.ListView>
""" % (VIEW, "\n".join("    <string>%s</string>" % j for j in JOBS))

_CJ = http.cookiejar.CookieJar()
_OPENER = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(_CJ))
_AUTH = "Basic " + base64.b64encode(f"{USER}:{PWD}".encode()).decode()


def request(method, path, data=None, ctype=None):
    r = urllib.request.Request(BASE + path, data=data, method=method)
    r.add_header("Authorization", _AUTH)
    if ctype:
        r.add_header("Content-Type", ctype)
    try:
        with _OPENER.open(r, timeout=60) as resp:
            return resp.status, resp.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def crumb():
    code, body = request("GET", "/crumbIssuer/api/json")
    if code != 200:
        return None, None
    d = json.loads(body)
    return d["crumbRequestField"], d["crumb"]


def post(path, data, ctype):
    f, v = crumb()
    r = urllib.request.Request(BASE + path, data=data, method="POST")
    r.add_header("Authorization", _AUTH)
    r.add_header("Content-Type", ctype)
    if f and v:
        r.add_header(f, v)
    try:
        with _OPENER.open(r, timeout=60) as resp:
            return resp.status, resp.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def main():
    quoted = urllib.parse.quote("职考云")
    code, body = post(f"/createView?name={quoted}", VIEW_XML.encode(), "application/xml")
    if code == 200:
        print("created view:", VIEW)
        return
    if "already exists" in body:
        code2, _ = post(f"/view/{quoted}/config.xml", VIEW_XML.encode(), "application/xml")
        print("view exists, update ->", code2)
        return
    print("create view failed:", code, body[:300])


if __name__ == "__main__":
    main()
