#!/usr/bin/env bash
# 全量部署：对每个服务调用 startup.sh（等价于职考云每个 zkxt-xxx job 的那条 ssh 命令）
# 2 核机器上 15 个服务同时启动会互相拖死（Nacos 配置拉取超时），所以分两轮：
#   第一轮全部启动 -> 等一段时间 -> 第二轮把没起来的 docker restart -> 再等
set -uo pipefail

ALL="user auth search media message course pay trade exam learning cert remark data promotion gateway"
SERVICES="${SERVICES:-$ALL}"
NETWORK="${NETWORK:-zkxt-net}"
BASE_PATH="${BASE_PATH:-$PWD}"
PROBE_HOST="${PROBE_HOST:-192.168.150.101}"
export BASE_PATH NETWORK

port_of() {
  case "$1" in
    user) echo 8082 ;; auth) echo 8081 ;; search) echo 8083 ;; media) echo 8084 ;;
    message) echo 8085 ;; course) echo 8086 ;; pay) echo 8087 ;; trade) echo 8088 ;;
    exam) echo 8089 ;; learning) echo 8090 ;; cert) echo 8091 ;; remark) echo 8092 ;;
    data) echo 8093 ;; promotion) echo 8094 ;; gateway) echo 10010 ;; *) echo "" ;;
  esac
}

# auth / message / pay 是多模块：目录 zk-<svc>/zk-<svc>-service，jar 名 zk-<svc>-service.jar
dir_of() {
  case "$1" in
    auth|message|pay) echo "zk-$1/zk-$1-service" ;;
    *) echo "zk-$1" ;;
  esac
}

jar_of() {
  case "$1" in
    auth|message|pay) echo "zk-$1-service" ;;
    *) echo "zk-$1" ;;
  esac
}

port_up() {
  (echo >"/dev/tcp/$PROBE_HOST/$1") >/dev/null 2>&1
}

# 网络不存在时自动创建，避免换机器部署时报 network not found
docker network create "$NETWORK" >/dev/null 2>&1 || true

echo "==> round 1: start $SERVICES"
for svc in $SERVICES; do
  bash docker/startup.sh -c "zkxt-$svc" -n "$(jar_of "$svc")" -d "$(dir_of "$svc")" -p "$(port_of "$svc")" \
    || echo "!! $svc startup.sh 失败"
  sleep 3
done

echo "==> waiting for services"
round=1
while [ $round -le 4 ]; do
  deadline=$((SECONDS + 300))
  while [ $SECONDS -lt $deadline ]; do
    left=""
    for svc in $SERVICES; do port_up "$(port_of "$svc")" || left="$left $svc"; done
    [ -z "$left" ] && break
    sleep 10
  done
  missing=""
  for svc in $SERVICES; do port_up "$(port_of "$svc")" || missing="$missing $svc"; done
  [ -z "$missing" ] && break
  echo "==> round $round: restart 未就绪:$missing"
  for svc in $missing; do
    docker restart "zkxt-$svc" >/dev/null 2>&1 || true
    sleep 3
  done
  round=$((round + 1))
done

echo "==> 结果:"
failed=""
for svc in $SERVICES; do
  port="$(port_of "$svc")"
  if port_up "$port"; then
    echo "   OK   $svc:$port"
  else
    echo "   FAIL $svc:$port"
    failed="$failed $svc"
  fi
done

echo "==> running containers:"
docker ps --filter 'name=zkxt-' --format '{{.Names}}\t{{.Status}}'

if [ -n "$failed" ]; then
  echo "!! 未就绪的服务:$failed"
  exit 1
fi
echo "==> all services up"
