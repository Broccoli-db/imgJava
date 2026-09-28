#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -eq 0 ]; then
  echo "用法: source scripts/setup-protected-roots.sh /path/to/read-only-root [...]" >&2
  return 1 2>/dev/null || exit 1
fi

resolved=()
for path in "$@"; do
  if [ ! -d "$path" ]; then
    echo "目录不存在: $path" >&2
    return 1 2>/dev/null || exit 1
  fi
  resolved+=("$(cd "$path" && pwd)")
done

joined=$(IFS=:; echo "${resolved[*]}")
export PROTECTED_READONLY_ROOTS="$joined"
echo "当前 shell 已配置受保护只读目录: $joined"
echo '请把等价 export 写入 shell 配置，供新启动的 AI 工具继承。'
