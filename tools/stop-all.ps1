# Stop all backend services started by this project (only java processes running zk-*.jar).
param([switch]$Quiet)

$procs = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like '*zk-*.jar*' }

if (-not $procs) {
    if (-not $Quiet) { Write-Host "No running service found." }
    exit 0
}

foreach ($p in $procs) {
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}
if (-not $Quiet) { Write-Host ("Stopped {0} service process(es)." -f $procs.Count) }
