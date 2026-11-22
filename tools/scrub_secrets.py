# -*- coding: utf-8 -*-
"""把仓库里硬编码的真实密钥替换成环境变量占位符。

只改「值」，保留缩进与 yaml 结构；按行号精确替换（appId 在 pay 里出现两次，不能按名字替换）。
"""
import io

ROOT = r"D:\Java\tianji"

# 文件 -> {行号: 新行内容(不含缩进，会保留原缩进)}
PLAN = {
    r"zk-media\src\main\resources\bootstrap.yml": {
        47: "appId: ${TENCENT_APP_ID:}",
        48: "secretId: ${TENCENT_SECRET_ID:}",
        49: "secretKey: ${TENCENT_SECRET_KEY:}",
        55: 'urlKey: "${TENCENT_VOD_URL_KEY:}"',
    },
    r"zk-pay\zk-pay-service\src\main\resources\bootstrap.yml": {
        38: "notifyHost: ${PAY_NOTIFY_HOST:http://localhost:8087/ps}",
        40: 'appId: "${ALIPAY_APP_ID:}"',
        41: 'merchantPrivateKey: "${ALIPAY_MERCHANT_PRIVATE_KEY:}"',
        42: 'publicKey: "${ALIPAY_PUBLIC_KEY:}"',
        44: 'appId: "${WXPAY_APP_ID:}"',
        45: 'mchId: "${WXPAY_MCH_ID:}"',
        46: 'mchSerialNo: "${WXPAY_MCH_SERIAL_NO:}"',
        47: 'privateKey: "${WXPAY_PRIVATE_KEY:}"',
        48: 'apiV3Key: "${WXPAY_API_V3_KEY:}"',
    },
    r"zk-message\zk-message-service\src\main\resources\bootstrap.yml": {
        52: "accessId: ${SMS_ACCESS_ID:}",
        53: "accessSecret: ${SMS_ACCESS_SECRET:}",
    },
}

for rel, mapping in PLAN.items():
    path = ROOT + "\\" + rel
    lines = io.open(path, encoding="utf-8").read().splitlines()
    changed = 0
    for lineno, new_text in mapping.items():
        idx = lineno - 1
        old = lines[idx]
        indent = old[: len(old) - len(old.lstrip())]
        lines[idx] = indent + new_text
        changed += 1
    io.open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    print("替换 %-60s %d 行" % (rel, changed))
