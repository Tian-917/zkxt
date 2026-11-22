# -*- coding: utf-8 -*-
"""审计：后端 Controller 暴露的端点，前端 api.js 实际用了哪些"""
import io
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

RE_CLASS = re.compile(r'@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"')
RE_METHOD = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\(\s*(?:value\s*=\s*)?"?([^")\s]*)"?')
RE_PLAIN = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\b(?!\()')

# 前端实际调用
api_text = io.open(os.path.join(ROOT, 'web', 'src', 'api.js'), encoding='utf-8').read()
api_paths = re.findall(r"request\(\s*[`'\"]([^`'\"?]*)", api_text)
api_paths += re.findall(r"request\(\s*`([^`]*)`", api_text)
api_joined = api_text.replace('${', '{')


def norm(p):
    p = re.sub(r'\{[^}]*\}', '{}', p)
    return p.strip('/')


api_norm = set()
for p in api_paths + [api_joined]:
    for seg in re.findall(r'/[a-zA-Z0-9\-/{}$]+', str(p)):
        api_norm.add(norm(seg))
full_api = api_text.replace('${', '{')


def is_used(fullpath):
    # 取路径最后两段做包含判断
    parts = [x for x in norm(fullpath).split('/') if not x.startswith('{')]
    if not parts:
        return True
    tail = parts[-1]
    if len(tail) < 3:
        return True
    if tail in full_api:
        return True
    if len(parts) >= 2 and (parts[-2] + '/' + parts[-1]) in full_api:
        return True
    return False


rows = []
for dirpath, dirnames, filenames in os.walk(ROOT):
    if '\\target\\' in dirpath + '\\' or '/target/' in dirpath or '.git' in dirpath:
        continue
    for fn in filenames:
        if not fn.endswith('Controller.java'):
            continue
        path = os.path.join(dirpath, fn)
        src = io.open(path, encoding='utf-8', errors='ignore').read()
        m = RE_CLASS.search(src)
        base = m.group(1) if m else ''
        module = os.path.relpath(path, ROOT).split(os.sep)[0]
        # 逐个方法
        for mm in re.finditer(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"?([^")\s]*)"?\s*\))?', src):
            verb, sub = mm.group(1), (mm.group(2) or '')
            full = '/' + '/'.join(x.strip('/') for x in [base, sub] if x.strip('/'))
            full = '/' + full.strip('/')
            rows.append({
                'module': module,
                'controller': fn.replace('.java', ''),
                'verb': verb.upper(),
                'path': full,
                'used': is_used(full),
            })

used = [r for r in rows if r['used']]
unused = [r for r in rows if not r['used']]

print('端点总数:', len(rows), ' 用到:', len(used), ' 未用:', len(unused))
by_mod = {}
for r in unused:
    by_mod.setdefault(r['module'], []).append(r)
for mod in sorted(by_mod):
    print('\n==', mod, '(%d)' % len(by_mod[mod]))
    for r in by_mod[mod]:
        print('   %-6s %-58s %s' % (r['verb'], r['path'], r['controller']))

io.open('audit.json', 'w', encoding='utf-8').write(json.dumps(
    {'total': len(rows), 'used': len(used), 'unused': unused}, ensure_ascii=False, indent=1))
