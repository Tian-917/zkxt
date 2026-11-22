# -*- coding: utf-8 -*-
"""检查前端调用到的 api.* 方法是否都还在 api.js 里导出"""
import io
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(ROOT, 'web', 'src')

api_src = io.open(os.path.join(WEB, 'api.js'), encoding='utf-8').read()
defined = set(re.findall(r'^\s{2}(\w+):', api_src, re.M))

used = {}
for dirpath, _, files in os.walk(WEB):
    for fn in files:
        if not fn.endswith(('.vue', '.js')) or fn == 'api.js':
            continue
        path = os.path.join(dirpath, fn)
        src = io.open(path, encoding='utf-8', errors='ignore').read()
        for m in re.finditer(r'api\.(\w+)\s*\(', src):
            used.setdefault(m.group(1), set()).add(fn)

missing = {k: v for k, v in used.items() if k not in defined}
print('api.js 导出:', len(defined), ' 被调用:', len(used))
if missing:
    print('\n缺失的方法（调用方会报 is not a function）：')
    for k, files in sorted(missing.items()):
        print('  %-24s <- %s' % (k, ', '.join(sorted(files))))
else:
    print('全部调用都能找到定义 [OK]')

unused = sorted(defined - set(used))
print('\n定义但没被调用（%d 个）: %s' % (len(unused), ', '.join(unused)))
