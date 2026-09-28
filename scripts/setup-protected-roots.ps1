param(
  [Parameter(Mandatory = $true)]
  [string[]]$Paths
)

$resolved = foreach ($path in $Paths) {
  if (-not (Test-Path -LiteralPath $path -PathType Container)) {
    throw "目录不存在: $path"
  }
  (Resolve-Path -LiteralPath $path).Path
}

$joined = $resolved -join [IO.Path]::PathSeparator
[Environment]::SetEnvironmentVariable('PROTECTED_READONLY_ROOTS', $joined, 'User')
$env:PROTECTED_READONLY_ROOTS = $joined

Write-Host "已配置受保护只读目录：$joined"
Write-Host '当前 PowerShell 已生效；其他已启动的 AI 工具需要重启。'
Write-Host '可运行 java scripts/TestGuard.java 验证 guard。'
