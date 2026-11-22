# -*- coding: utf-8 -*-
"""生成带请求参数与响应 schema 的 OpenAPI 3.0.1（供 Apifox 导入）。"""
import io
import json
import os
import re

ROOT = "."
OUT = "apifox/zkxt-openapi.json"

PREFIX = {"auth": "as", "user": "us", "search": "ss", "course": "cs", "learning": "ls",
          "exam": "es", "cert": "cts", "trade": "ts", "pay": "ps", "promotion": "prs",
          "remark": "rs", "media": "ms", "message": "sms", "data": "ds"}
SVC_CN = {"auth": "认证服务", "user": "用户服务", "search": "搜索服务", "course": "课程服务",
          "learning": "学习服务", "exam": "考试服务", "cert": "证书服务", "trade": "交易服务",
          "pay": "支付服务", "promotion": "促销服务", "remark": "评论服务", "media": "媒资服务",
          "message": "消息服务", "data": "数据服务"}

MODULE = {s: (f"zk-{s}/zk-{s}-service" if os.path.isdir(f"zk-{s}/zk-{s}-service") else f"zk-{s}") for s in PREFIX}

# ---------- 收集全部 java 源文件 ----------
JAVA = {}
for dp, _d, fs in os.walk("."):
    if any(x in dp for x in (".git", "node_modules", "target", "apifox")):
        continue
    for f in fs:
        if f.endswith(".java"):
            JAVA.setdefault(f[:-5], []).append(os.path.join(dp, f))


def read(name):
    paths = JAVA.get(name) or []
    return io.open(paths[0], encoding="utf-8", errors="ignore").read() if paths else ""


PRIM = {
    "int": ("integer", "int32"), "Integer": ("integer", "int32"),
    "long": ("integer", "int64"), "Long": ("integer", "int64"),
    "short": ("integer", "int32"), "byte": ("integer", "int32"),
    "float": ("number", "float"), "double": ("number", "double"),
    "Double": ("number", "double"), "BigDecimal": ("number", "double"),
    "boolean": ("boolean", None), "Boolean": ("boolean", None),
    "String": ("string", None), "CharSequence": ("string", None),
    "LocalDateTime": ("string", "date-time"), "LocalDate": ("string", "date"),
    "Date": ("string", "date-time"), "LocalTime": ("string", None),
    "Object": ("object", None), "Map": ("object", None), "MultipartFile": ("string", "binary"),
}

SCHEMAS = {}


def schema_of(type_str, field=None):
    t = re.sub(r"\s+", "", (type_str or ""))
    # 分页包装类：PageDTO<CourseVO> -> {total, pages, list: CourseVO[]}
    if t.startswith("PageDTO<"):
        inner = t[t.index("<") + 1:t.rindex(">")]
        return {"type": "object", "properties": {
            "total": {"type": "integer", "format": "int64"},
            "pages": {"type": "integer", "format": "int64"},
            "list": {"type": "array", "items": schema_of(inner)},
        }}
    if t.startswith("List<") or t.startswith("Set<") or t.startswith("Collection<"):
        inner = t[t.index("<") + 1:t.rindex(">")]
        return {"type": "array", "items": schema_of(inner)}
    if t.startswith("Map<") or t == "Map":
        return {"type": "object"}
    if t.endswith("[]"):
        return {"type": "array", "items": schema_of(t[:-2])}
    base = t.split("<")[0].strip()
    if base in PRIM:
        typ, fmt = PRIM[base]
        s = {"type": typ}
        if fmt:
            s["format"] = fmt
        return s
    if base in SCHEMAS or read(base):
        ensure_schema(base)
        return {"$ref": "#/components/schemas/" + base}
    return {"type": "object", "description": "Java 类型: " + base}


FIELD_RE = re.compile(r"(?:@ApiModelProperty\(\s*(?:value\s*=\s*)?\"([^\"]*)\"[^)]*\)\s*)?private\s+(?!static)([\w<>\[\],.\s]+?)\s+(\w+)\s*;")


def ensure_schema(name, depth=0):
    if name in SCHEMAS or depth > 3:
        return
    src = read(name)
    if not src:
        SCHEMAS[name] = {"type": "object"}
        return
    SCHEMAS[name] = {"type": "object", "properties": {}}
    props = SCHEMAS[name]["properties"]
    for m in FIELD_RE.finditer(src):
        desc, typ, fname = m.group(1), m.group(2), m.group(3)
        p = schema_of(typ, fname)
        if desc:
            p["description"] = desc
        props[fname] = p
    # 去掉空对象
    if not props:
        SCHEMAS[name] = {"type": "object"}


# ---------- 解析控制器 ----------
REQMAP = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?(?:path\s*=\s*)?"([^"]+)"')
MAPPING = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\b')
APIOP = re.compile(r'@ApiOperation\(\s*(?:value\s*=\s*)?"([^"]+)"')
APITAG = re.compile(r'@Api\(\s*(?:tags\s*=\s*|value\s*=\s*)?"([^"]+)"')
SECURED = re.compile(r'@ApiOperation')
NOAUTH = ("login", "code", "register", "refresh", "password", "verify", "captcha")


NOUN = {"questions": "题目", "replies": "回答", "course": "课程", "courses": "课程", "boards": "榜单",
        "jwks": "密钥", "orders": "订单", "refunds": "退款", "users": "用户", "cert": "证书",
        "coupons": "优惠券", "notes": "笔记", "exams": "考试", "sections": "小节", "categories": "分类",
        "teachers": "讲师", "students": "学员", "media": "视频", "points": "积分"}


def fallback_summary(verb, path):
    act = {"get": "查询", "post": "新增", "put": "修改", "delete": "删除", "patch": "修改"}.get(verb, "操作")
    parts = [p for p in path.strip("/").split("/")
             if p and not p.startswith("{") and p not in ("admin", "page", "list", "save", "update", "delete", "hidden")]
    for p in reversed(parts):
        if p in NOUN:
            return act + NOUN[p]
    return act + (parts[-1] if parts else "接口")


def split_params(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    return out


def parse_ops(path, svc):
    text = io.open(path, encoding="utf-8").read()
    if "@RestController" not in text:
        return []
    # @ApiIgnore 的控制器（支付回调等内部接口）不进接口文档
    if "@ApiIgnore" in text:
        return []
    cm = REQMAP.search(text)
    base = cm.group(1) if cm else ""
    tm = APITAG.search(text)
    api_tag = tm.group(1) if tm else "相关接口"
    ops = []
    for m in MAPPING.finditer(text):
        verb = m.group(1).lower()
        # 取注解后的路径
        pm = re.match(r'\(\s*(?:value\s*=\s*)?(?:path\s*=\s*)?"?([^")]*)"?\s*\)', text[m.end():])
        sub = (pm.group(1) or "") if pm else ""
        # 方法签名：注解后到第一个 '{'
        rest = text[m.end():]
        brace = rest.find("{")
        sig = rest[:brace] if brace > 0 else rest[:600]
        sig = re.sub(r"\s+", " ", sig)
        # 从右往左找到方法参数列表的括号（避免匹配到注解里的括号）
        end = sig.rfind(")")
        params_src, before = "", sig
        if end > 0:
            depth, i = 0, end
            while i >= 0:
                if sig[i] == ")":
                    depth += 1
                elif sig[i] == "(":
                    depth -= 1
                    if depth == 0:
                        break
                i -= 1
            if i >= 0:
                params_src = sig[i + 1:end]
                before = sig[:i]
        before = re.sub(r"@\w+(\([^)]*\))?", "", before)  # 去掉注解，只留 修饰符+返回类型+方法名
        ret = ""
        mm = re.search(r"([\w<>\[\],.\s]+?)\s+(\w+)\s*$", before.strip())
        if mm:
            ret = mm.group(1).strip()
            # 去掉访问修饰符，只留返回类型
            ret = re.sub(r"\b(public|private|protected|static|final|synchronized|abstract|default)\b", "", ret).strip()
        # @ApiOperation 通常写在 @GetMapping 之前，所以先往前找最近的，再往后找
        am = None
        before_txt = text[max(0, m.start() - 600):m.start()]
        prev = APIOP.findall(before_txt)
        if prev:
            summary = prev[-1]
        else:
            am = APIOP.search(text[m.end():m.end() + 600])
            summary = am.group(1) if am else fallback_summary(verb, (base.rstrip("/") + "/" + sub.lstrip("/")))
        # 路径参数
        pnames = re.findall(r"\{(\w+)\}", sub)
        parameters, body = [], None
        for p in split_params(params_src):
            p = p.strip()
            if not p or "HttpServletRequest" in p or "HttpServletResponse" in p:
                continue
            pv = re.search(r'@PathVariable(?:\("([^"]*)"\))?', p)
            rq = re.search(r'@RequestParam(?:\("([^"]*)"\))?', p)
            rb = "@RequestBody" in p
            dm = re.search(r'defaultValue\s*=\s*"([^"]*)"', p)
            tm2 = re.search(r"([\w<>\[\],.]+)\s+(\w+)\s*$", p)
            if not tm2:
                continue
            jtype, jname = tm2.group(1), tm2.group(2)
            if rb:
                body = jtype
                continue
            qname = (pv.group(1) if pv and pv.group(1) else None) or (rq.group(1) if rq and rq.group(1) else None) or jname
            sch = schema_of(jtype)
            prm = {"name": qname, "in": "path" if pv else "query", "required": bool(pv) or "required=true" in p,
                   "schema": sch}
            if dm:
                prm["schema"]["default"] = dm.group(1)
                prm["required"] = False
            parameters.append(prm)
        full = "/" + PREFIX[svc] + "/" + (base.strip("/") + "/" + sub.strip("/")).strip("/")
        full = re.sub(r"/{2,}", "/", full).rstrip("/") or "/"
        op = {"summary": summary, "tags": ["%s-%s" % (svc, SVC_CN.get(svc, "服务"))],
              "x-apifox-folder": "%s-%s/%s" % (svc, SVC_CN.get(svc, "服务"), api_tag),
              "responses": {}}
        if parameters:
            op["parameters"] = parameters
        if body:
            op["requestBody"] = {"required": True,
                                 "content": {"application/json": {"schema": schema_of(body)}}}
        # 响应：R<T> 包装
        inner = None
        rm = re.search(r"<(.*)>", ret or "")
        if rm:
            inner = rm.group(1)
        data_schema = schema_of(inner) if inner and inner not in ("", "?") else ({"type": "object"} if ret and ret not in ("void", "R") else None)
        resp = {"code": {"type": "integer", "format": "int32", "example": 200},
                "msg": {"type": "string", "example": "OK"}}
        if data_schema:
            resp["data"] = data_schema
        op["responses"]["200"] = {
            "description": "OK",
            "content": {"application/json": {"schema": {"type": "object", "properties": resp}}},
        }
        if not any(k in full for k in NOAUTH):
            op["security"] = [{"Authorization": []}]
        ops.append((full, verb, op))
    return ops


paths = {}
count = 0
for svc in PREFIX:
    d = os.path.join(MODULE[svc], "src", "main", "java")
    if not os.path.isdir(d):
        continue
    for dp, _dirs, fs in os.walk(d):
        if "controller" not in dp.lower():
            continue
        for f in fs:
            if f.endswith(".java"):
                for full, verb, op in parse_ops(os.path.join(dp, f), svc):
                    op["operationId"] = "%s_%s_%d" % (f[:-5].lower(), verb, count)
                    paths.setdefault(full, {})[verb] = op
                    count += 1

doc = {
    "openapi": "3.0.1",
    "info": {"title": "职考云项目",
             "description": "职考云 · 职业技能考证平台接口文档（从控制器与 DTO 代码生成）",
             "version": "1.0.0"},
    "tags": [{"name": "%s-%s" % (s, SVC_CN[s])} for s in PREFIX],
    "components": {
        "securitySchemes": {"Authorization": {"type": "apiKey", "in": "header", "name": "authorization",
                                               "description": "登录接口返回的 JWT，直接放这个头（不加 Bearer）"}},
        "schemas": dict(sorted(SCHEMAS.items())),
    },
    "paths": dict(sorted(paths.items())),
}
os.makedirs("apifox", exist_ok=True)
with io.open(OUT, "w", encoding="utf-8") as f:
    json.dump(doc, f, ensure_ascii=False, indent=2)

with_body = sum(1 for p in paths.values() for o in p.values() if "requestBody" in o)
with_param = sum(1 for p in paths.values() for o in p.values() if "parameters" in o)
print("接口数:", count, "| 路径数:", len(paths))
print("带请求体:", with_body, "| 带查询/路径参数:", with_param, "| schema 数:", len(SCHEMAS))
print("文件:", OUT, round(os.path.getsize(OUT) / 1024, 1), "KB")
