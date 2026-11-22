#!/usr/bin/env bash
# 把 mvn 打出来的 fat jar 逐个打成镜像：zkxt/<service>:<tag>
# 由 Jenkinsfile 在 Build 之后调用，也可以本地手动跑。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

TAG="${TAG:-${BUILD_NUMBER:-latest}}"
SERVICES="${SERVICES:-user auth search media message course pay trade exam learning cert remark data promotion gateway}"

# auth / message / pay 是多模块，jar 叫 zk-<svc>-service.jar；其余是 zk-<svc>.jar
find_jar() {
  local svc="$1"
  for name in "zk-${svc}-service.jar" "zk-${svc}.jar"; do
    local hit
    hit="$(find "$ROOT" -maxdepth 5 -path "*/target/$name" -not -path "*/dependency/*" | head -1)"
    if [ -n "$hit" ]; then
      echo "$hit"
      return 0
    fi
  done
  return 1
}

built=0
for svc in $SERVICES; do
  jar="$(find_jar "$svc")" || { echo "!! 找不到 $svc 的 jar，跳过"; continue; }
  rel="${jar#"$ROOT"/}"
  echo "==> building zkxt/$svc:$TAG  <- $rel"
  docker build -q -f docker/Dockerfile --build-arg "JAR_FILE=$rel" -t "zkxt/$svc:$TAG" -t "zkxt/$svc:latest" "$ROOT"
  built=$((built + 1))
done

echo "OK: built $built image(s), tag=$TAG"
docker images --format '{{.Repository}}:{{.Tag}}  {{.Size}}' | grep '^zkxt/' | sort
