#!/usr/bin/env bash
# 仿照职考云 /usr/local/src/script/startup.sh：
#   1) 从 workspace 里拷贝该服务的 fat jar 成 app.jar
#   2) docker build 出 <容器名>:latest
#   3) 删旧容器后 docker run（固定内存 300m、接入 zkxt-net、发布端口）
#
# 用法: startup.sh -c zkxt-course -n zk-course -d zk-course -p 8086 [-o "-Xms300m -Xmx300m"]
set -e

BASE_PATH="${BASE_PATH:-$PWD}"
PROJECT_NAME=""
PROJECT_PATH=""
CONTAINER_NAME=""
JAVA_OPTS="-Xms96m -Xmx192m -XX:MaxMetaspaceSize=112m"
PORT=8080
NETWORK="${NETWORK:-zkxt-net}"

while getopts "c:n:d:p:o:" opt; do
  case "$opt" in
    c) CONTAINER_NAME=$OPTARG ;;
    n) PROJECT_NAME=$OPTARG ;;
    d) PROJECT_PATH=$OPTARG ;;
    p) PORT=$OPTARG ;;
    o) [ -n "$OPTARG" ] && JAVA_OPTS=$OPTARG ;;
    *) echo "unknown argument"; exit 1 ;;
  esac
done

IMAGE_NAME="${CONTAINER_NAME}:latest"
JAR="${BASE_PATH}/${PROJECT_PATH}/target/${PROJECT_NAME}.jar"

echo "==> ${CONTAINER_NAME}: copy ${JAR}"
[ -f "$JAR" ] || { echo "!! jar 不存在: $JAR"; exit 1; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
cp "$JAR" "$WORK/app.jar"
cp "${BASE_PATH}/docker/Dockerfile.runtime" "$WORK/Dockerfile"

echo "==> ${CONTAINER_NAME}: build image"
docker build -q -t "$IMAGE_NAME" "$WORK" >/dev/null

echo "==> ${CONTAINER_NAME}: (re)create container, port ${PORT}"
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
docker run -d --name "$CONTAINER_NAME" \
  -p "${PORT}:${PORT}" \
  -e JAVA_OPTS="$JAVA_OPTS" \
  --memory 384m --memory-swap -1 \
  --network "$NETWORK" \
  "$IMAGE_NAME" >/dev/null

echo "==> ${CONTAINER_NAME}: up (image=${IMAGE_NAME})"
