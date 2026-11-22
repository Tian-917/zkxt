# 本地启动单个服务（JDK 17 兼容参数）
#
# 课程项目原本跑在 JDK 11 + Docker（LANG=C.UTF-8）里，所以不需要这些参数。
# 在 Windows + JDK 17 本地跑必须加：
#   -Dfile.encoding=UTF-8   否则 Nacos 上带中文注释的 shared-*.yaml 解码失败，整份配置被丢弃
#   --add-opens ...         否则 Seata(cglib)、MyBatis(OGNL) 反射被模块系统拦截
#
# 用法：
#   .\run-service.ps1 course
#   .\run-service.ps1 gateway
#   .\run-service.ps1 learning

param(
    [Parameter(Mandatory = $true)]
    [string]$Name,

    # 注册到 Nacos 的 IP。bootstrap-dev.yml 里写死的是课程 VM 的 192.168.150.101，
    # 本机跑多个服务时如果沿用，服务之间会互相连到 VM 上（那里没有进程）导致 Connection refused。
    # 本机联调：127.0.0.1；只想把本机服务暴露给 VM 其他服务：填本机局域网 IP。
    [string]$DiscoveryIp = "127.0.0.1"
)

$root = Join-Path $PSScriptRoot ".."
# 优先 zk-<name>-service.jar（auth/message/pay 是多模块），其次 zk-<name>.jar
$jar = $null
foreach ($candidate in @("zk-$Name-service.jar", "zk-$Name.jar")) {
    $jar = Get-ChildItem -Path $root -Recurse -Filter $candidate -ErrorAction SilentlyContinue |
           Where-Object { $_.FullName -notmatch '\\target\\dependency\\' } |
           Select-Object -First 1
    if ($jar) { break }
}

if (-not $jar) {
    Write-Host "找不到 zk-$Name.jar，先执行：mvn -T 1C -DskipTests package" -ForegroundColor Red
    exit 1
}

$flags = @(
    "-Dfile.encoding=UTF-8",
    "-Dsun.jnu.encoding=UTF-8",
    "-Dspring.cloud.nacos.discovery.ip=$DiscoveryIp",
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/java.math=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.nio=ALL-UNNAMED"
)

Write-Host "启动 $($jar.Name) ..." -ForegroundColor Cyan
& java @flags -jar $jar.FullName
