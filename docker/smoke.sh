#!/usr/bin/env bash
# 部署后冒烟：网关 + 门户 + 登录。刚重启的服务需要时间，所以每项都带重试。
set -uo pipefail

GW="${GW:-http://192.168.150.101:10010}"
RETRY="${RETRY:-30}"     # 重试次数
INTERVAL="${INTERVAL:-10}" # 每次间隔秒
fail=0

# check <名称> <url> <期望码>
check() {
  local name="$1" url="$2" expect="${3:-200}" code=""
  local i=0
  while [ $i -lt "$RETRY" ]; do
    code="$(curl -s -o /dev/null -m 15 -w '%{http_code}' "$url" 2>/dev/null)"
    [ -z "$code" ] && code="000"
    [ "$code" = "$expect" ] && { echo "PASS $name ($code)"; return 0; }
    i=$((i + 1)); sleep "$INTERVAL"
  done
  echo "FAIL $name (got $code, want $expect)"
  fail=$((fail + 1))
}

check "网关文档" "$GW/doc.html"
check "门户课程" "$GW/ss/courses/portal?pageNo=1&pageSize=1"

# 登录：等到返回里带 data
login_ok=0
i=0
while [ $i -lt "$RETRY" ]; do
  body="$(curl -s -m 15 -X POST "$GW/as/accounts/login" -H 'Content-Type: application/json' \
    -d '{"type":1,"username":"jack","password":"123456","rememberMe":true}' 2>/dev/null)"
  case "$body" in
    *'"data"'*) login_ok=1; break ;;
  esac
  i=$((i + 1)); sleep "$INTERVAL"
done
if [ "$login_ok" = "1" ]; then
  echo "PASS 学员登录"
else
  echo "FAIL 学员登录"
  echo "$body" | head -c 200
  fail=$((fail + 1))
fi

echo "----"
echo "smoke failures: $fail"
exit "$fail"