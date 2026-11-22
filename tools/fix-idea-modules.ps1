# 重置 IDEA 的模块模型（修复 "java 文件显示咖啡杯图标" 的问题）
#
# 背景：项目目录从 tj-* 改名成 zk-* 之后，IDEA 的外部模块模型里还是旧名字
#       （external_build_system\tjxt.xml 记着 26 个 tj-* 模块），
#       这些模块在新目录里找不到源码 → .java 文件不属于任何模块 → 只显示"Java 文件"图标。
#
# 用法：
#   1. 先完全关闭 IDEA（否则文件被占用）
#   2. powershell -ExecutionPolicy Bypass -File tools\fix-idea-modules.ps1
#   3. 重新打开项目，等 Maven 导入完成

$ErrorActionPreference = 'Stop'

# 1) 检查 IDEA 是否还在运行
$idea = Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.ProcessName -match 'idea64|idea' }
if ($idea) {
    Write-Host "IDEA 还在运行（PID: $($idea.Id -join ',')），请先完全退出再执行本脚本。" -ForegroundColor Yellow
    exit 1
}

# 2) 定位项目
$projectDir = Split-Path -Parent $PSScriptRoot
$projectName = Split-Path -Leaf $projectDir
Write-Host "项目目录: $projectDir"

$storageRoot = Join-Path $env:LOCALAPPDATA "JetBrains\IntelliJIdea2025.3\projects"
if (-not (Test-Path $storageRoot)) {
    Write-Host "找不到 IDEA 外部存储目录：$storageRoot" -ForegroundColor Yellow
    Write-Host "（不同 IDEA 版本路径不同，请手动查找 projects\<项目名>.<hash>\external_build_system 并删除）"
    exit 1
}

$target = Get-ChildItem $storageRoot -Directory | Where-Object { $_.Name -like "$projectName.*" } | Select-Object -First 1
if (-not $target) {
    Write-Host "在 $storageRoot 下没找到 $projectName.* ，可能项目路径变了。" -ForegroundColor Yellow
    exit 1
}

# 3) 备份 .idea
$ideaDir = Join-Path $projectDir '.idea'
if (Test-Path $ideaDir) {
    $backup = Join-Path $projectDir ('.idea.bak-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    Copy-Item $ideaDir $backup -Recurse -Force
    Write-Host "已备份 .idea -> $(Split-Path -Leaf $backup)"
}

# 4) 删除外部模块模型与索引标记（IDEA 会重新从 pom.xml 导入）
foreach ($sub in @('external_build_system', 'project-model-cache', 'indexingStamp')) {
    $p = Join-Path $target.FullName $sub
    if (Test-Path $p) {
        Remove-Item $p -Recurse -Force
        Write-Host "已删除 $sub"
    }
}

# 5) 清掉 .idea 里残留的旧模块名（compiler.xml 的 module 列表）
$compilerXml = Join-Path $ideaDir 'compiler.xml'
if (Test-Path $compilerXml) {
    $legacy = Select-String -Path $compilerXml -Pattern 'module name="tj-' -AllMatches
    if ($legacy) {
        Remove-Item $compilerXml -Force
        Write-Host "已删除 .idea\compiler.xml（里面有旧 tj-* 模块名，IDEA 会重新生成）"
    }
}

Write-Host ""
Write-Host "完成。现在重新打开项目：" -ForegroundColor Green
Write-Host "  1. 等右下角 'Maven importing' 跑完"
Write-Host "  2. 若仍有模块缺失，右键根 pom.xml → Add as Maven Project"
Write-Host "  3. 检查 zk-course/src/main/java 是否是蓝色文件夹（Sources Root）"
