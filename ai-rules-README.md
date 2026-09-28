# Java / Spring Boot 通用 AI 开发规则模板

这是一套面向 Java 后端项目、与具体 AI 厂商无关的协作规则。它可以用于 Codex、Claude Code、Cursor、Cline、GitHub Copilot、Windsurf 等工具；统一入口是 `AGENTS.md`。

## 设计目标

- 用一份跨工具入口描述项目事实、安全边界和工程约定。
- 把稳定架构、临时任务状态、专项规则和验证清单分开维护。
- 覆盖 Spring Boot API、持久化与迁移、配置、并发、认证授权、日志和优雅关闭。
- 不依赖任何单一工具的 Hook、Slash Command、Skill 或 Agent 私有格式。
- 为支持外部脚本的工具提供可选路径 guard，但不把它宣传成完整沙箱。

## 文件结构

```text
project-rules-template-java/
├── AGENTS.md                       # 所有 AI 工具的统一入口
├── AI.md                           # 加载顺序与工具适配说明
├── AI.local.md.example             # 本地个人配置示例
├── PROJECT.md                      # 项目目标、技术栈和质量闸门
├── .ai/
│   ├── CORE.md                     # 红线
│   ├── ARCHITECTURE.md             # 稳定架构知识
│   ├── CHECKLIST.md                # 完成前检查
│   ├── SESSION.md.example          # 本地任务状态
│   ├── rules/                      # Java 后端、测试、项目边界规则
│   ├── roles/                      # 工具无关角色提示
│   ├── workflows/                  # 初始化、审查、总结流程
│   ├── playbooks/                  # 验证手册
│   └── guards/                     # 可独立调用的保护脚本（Java 单文件）
├── docs/                           # 开发与测试规范、改动记录
├── scripts/                        # guard 自测和只读目录配置
├── install.ps1
├── install.sh
└── gitignore.snippet
```

## 安装

Windows：

```powershell
.\install.ps1 -Target "D:\path\to\java-project"
```

macOS/Linux：

```bash
bash install.sh /path/to/java-project
```

安装器不会覆盖已有规则文件，并会逐条合并 `.gitignore`。安装后，把 `.ai/workflows/init-project.md` 的内容交给当前 AI 工具执行，让它根据目标项目补全占位符并删除不适用规则。

## 工具接入

优先使用原生支持的 `AGENTS.md`。如果某个工具只识别自己的规则文件，创建一个很薄的适配文件即可，例如：

```md
# Project AI instructions

Read and follow `AGENTS.md` and `AI.md`. Load matching files under `.ai/rules/` for the current task.
```

不要把整套规则复制进多个厂商文件，否则很快会产生冲突和漂移。

## 外部边界

guard 对 `--path` 有两道独立判定，任一命中即拒绝：

**1. 项目目录之外一律只读** —— 对应 `.ai/CORE.md` 的最高红线。项目根取自 `PROJECT_ROOT` 环境变量，
未设置时回退到进程工作目录，因此**在项目根目录直接运行 guard 就生效，零配置**。用户明确授权跨项目
改动后，设 `ALLOW_WRITES_OUTSIDE_PROJECT=true` 放行这一道判定。

**2. 显式配置的外部只读目录** —— 用于把共享库或其他仓库额外标为禁区：

```powershell
.\scripts\setup-protected-roots.ps1 -Paths "D:\backend", "D:\shared-lib"
```

```bash
source scripts/setup-protected-roots.sh /path/to/backend /path/to/shared-lib
```

独立检查：

```bash
java .ai/guards/ProtectProjectBoundaries.java --path /path/to/file
java .ai/guards/ProtectProjectBoundaries.java --command "rm /path/to/file"
java scripts/TestGuard.java
```

退出码 `0` 放行、`2` 拒绝、`1` 参数错误，结果以 JSON 写到 stdout。

`--command` 模式**只做第 2 项判定，不套用项目边界**：命令字符串无法区分读和写，把「提到外部绝对路径
的命令」一律拦下，会连 `cat /别处/文件` 这种合法读取也误伤。

guard 使用 JDK 单文件源码模式（JDK 11+ 可用），不需要为目标项目引入任何依赖，也不要求目标机器安装 Node.js。它只识别显式路径：环境变量、符号链接、脚本间接写入或未调用 guard 的工具无法被它保护；严格隔离应使用操作系统权限、只读挂载或沙箱。

## 占位符

初始化后，项目规则文件中不应残留 `{{...}}` 或 `[待补全]`，无法确认的事实保留为 `[待确认]`。

常见占位符包括：

- `{{PROJECT_NAME}}`、`{{PROJECT_DESCRIPTION}}`、`{{TECH_STACK}}`
- `{{JDK_VERSION}}`、`{{BUILD_TOOL}}`、`{{FRAMEWORK}}`、`{{RUNTIME_TARGET}}`
- `{{DATABASE_STACK}}`、`{{MIGRATION_TOOL}}`、`{{VALIDATION}}`、`{{TEST_STACK}}`
- `{{DEV_COMMANDS}}`、`{{TEST_COMMANDS}}`、`{{DIRECTORY_STRUCTURE}}`、`{{EXTERNAL_READONLY}}`
- `{{TIMESTAMP}}`（`YYYY-MM-DD HH:mm:ss`，精确到秒）

## 使用原则

- 规则要短、具体、可验证；项目事实以代码和正式契约为准。
- 只保留项目真正使用的技术规则，不把模板示例当事实。
- 修改任务更新 `docs/记录.md`；只读审查、总结和纯验证不写记录。
- 模板升级时人工审查差异，不自动覆盖目标项目已有规则。
