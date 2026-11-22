#!/bin/sh
# 在虚拟机宿主机 nginx 上启用 *.zkxt.com 虚拟主机（幂等，可重复执行）
#
# 用法（在能访问 docker API 的机器上）：
#   VM_PID=host python tools/vm_run.py redis "/:/host" \
#     "echo $(base64 -w0 docker/nginx-zkxt.conf) | base64 -d > /tmp/zkxt.conf && sh /tmp/setup-nginx.sh"
#
# 做的事：
#   1) 把 conf/zkxt.conf 写到宿主机 nginx 配置目录
#   2) 在 nginx.conf 的 http{} 末尾加一行 include zkxt.conf;（已加过则跳过）
#   3) nginx -t 校验，通过后 reload
set -e
N=/host/usr/local/src/nginx          # 从容器里看宿主机的路径
INNER=/usr/local/src/nginx           # chroot /host 之后的路径
CNF="$N/conf/nginx.conf"

cp /tmp/zkxt.conf "$N/conf/zkxt.conf"
echo "== 写入 $N/conf/zkxt.conf"

if grep -qE 'include[[:space:]]+zkxt\.conf;' "$CNF"; then
  echo "== include 已存在，跳过"
else
  cp "$CNF" "$CNF.bak.zkxt"
  head -n -1 "$CNF" > "$CNF.new"
  echo '    include       zkxt.conf;' >> "$CNF.new"
  tail -n 1 "$CNF" >> "$CNF.new"
  mv "$CNF.new" "$CNF"
  echo "== 已插入 include（备份 nginx.conf.bak.zkxt）"
fi

echo "== nginx -t"
chroot /host "$INNER/sbin/nginx" -p "$INNER" -c conf/nginx.conf -t 2>&1
echo "== reload"
chroot /host "$INNER/sbin/nginx" -p "$INNER" -c conf/nginx.conf -s reload 2>&1
echo "== done"
