# Start all 15 backend services. Logs go to logs\<name>.log / .err (logs/ is gitignored).
# ASCII-only output on purpose: Windows PowerShell 5.1 reads BOM-less .ps1 as ANSI,
# which garbles Chinese messages.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\start-all.ps1
#   powershell -ExecutionPolicy Bypass -File tools\start-all.ps1 -Only user,auth,gateway

param(
    [string[]]$Only = @(),
    [int]$WaitSeconds = 70
)

$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
$logsDir = Join-Path $projectDir 'logs'
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null
$runScript = Join-Path $PSScriptRoot 'run-service.ps1'

$all = @('user', 'auth', 'search', 'course', 'learning', 'exam', 'cert', 'gateway',
         'trade', 'promotion', 'pay', 'message', 'media', 'remark', 'data')

$services = @()
if ($Only.Count -gt 0) { $services = $Only } else { $services = $all }

Write-Host ("Starting {0} service(s). Logs: {1}" -f $services.Count, $logsDir)
foreach ($s in $services) {
    if ($all -notcontains $s) {
        Write-Host ("  skip unknown service: {0}" -f $s) -ForegroundColor Yellow
        continue
    }
    $out = Join-Path $logsDir ($s + '.log')
    $err = Join-Path $logsDir ($s + '.err')
    Remove-Item $out, $err -ErrorAction SilentlyContinue

    $argList = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $runScript, $s)
    Start-Process -FilePath 'powershell' -ArgumentList $argList -WorkingDirectory $projectDir -RedirectStandardOutput $out -RedirectStandardError $err -WindowStyle Hidden
    Write-Host ("  -> {0}" -f $s)
    Start-Sleep -Seconds 3
}

if ($WaitSeconds -gt 0) {
    Write-Host ("Waiting {0}s for services to become ready..." -f $WaitSeconds)
    Start-Sleep -Seconds $WaitSeconds
}

$ports = @{
    user = 8082; auth = 8081; search = 8083; course = 8086; learning = 8090; exam = 8089
    cert = 8091; gateway = 10010; trade = 8088; promotion = 8094; pay = 8087
    message = 8085; media = 8084; remark = 8092; data = 8093
}
$down = @()
foreach ($s in $services) {
    if (-not $ports.ContainsKey($s)) { continue }
    $p = $ports[$s]
    $ok = Test-NetConnection -ComputerName 127.0.0.1 -Port $p -WarningAction SilentlyContinue -InformationLevel Quiet
    if (-not $ok) { $down += ($s + '(' + $p + ')') }
}
if ($down.Count -eq 0) {
    Write-Host "All services are up." -ForegroundColor Green
} else {
    Write-Host ("Not ready: {0}  -- see logs\<service>.log" -f ($down -join ', ')) -ForegroundColor Yellow
}
