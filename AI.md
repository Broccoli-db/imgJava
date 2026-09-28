# imgJava AI 开发入口

本文件是工具无关的项目说明。AI 工具若只自动识别一种规则文件，应优先加载 `AGENTS.md`，并把本文件及 `.ai/` 下的相关规则加入项目上下文。

## 建议加载顺序

1. `AGENTS.md`：跨工具统一入口
2. `.ai/CORE.md`：安全红线和基本行为
3. `.ai/ARCHITECTURE.md`：稳定架构事实
4. `.ai/SESSION.md`：本地任务状态（存在时）
5. `AI.local.md`：个人项目偏好（存在时）
6. `PROJECT.md`：项目目标、技术栈和命令
7. 与目标路径匹配的 `.ai/rules/*.md`
8. `docs/记录.md` 顶部最近 3 条

不要无目的加载全部长文档；先按任务和路径选择相关规则。各工具的自动发现机制不同，本模板不假设某个厂商的专有 Hook、Command 或 Agent 协议。

## 常用命令

```bash
docker compose up -d                     # 启动本地 PostgreSQL / MongoDB / Redis
./mvnw spring-boot:run                   # 启动应用
./mvnw test                              # 运行测试（Testcontainers 会另起容器）
./mvnw -Dtest=ImgJavaApplicationTests test   # 只跑指定测试
```

Windows 原生终端使用 `mvnw.cmd` 替代 `./mvnw`。

## 高层架构

```text
src/main/java/com/imgjava/
  ImgJavaApplication.java          # 应用入口
src/main/resources/
  application.properties           # 全部配置，尚未按 profile 拆分
  db/migration/                    # Flyway 迁移脚本
  static/  templates/
src/test/java/com/imgjava/
  ImgJavaApplicationTests.java     # 上下文加载测试（Testcontainers）
  TestcontainersConfiguration.java # PostgreSQL/MongoDB/Redis 容器定义
  TestImgJavaApplication.java      # 用容器启动应用的本地入口
```

业务分层尚未建立，推荐分层见 `docs/开发规范.md` 第 2 节。

## 全局约束

- 使用项目声明的 JDK 25 和构建包装器 `./mvnw`，不混用 Maven 与 Gradle。
- 保持现有构建体系与依赖声明风格，不在无关任务中迁移构建工具。
- API、数据库、缓存、消息和第三方服务契约以代码、迁移和正式文档为准。
- 配置启动时校验；日志脱敏；输入在边界验证；权限在资源层确认。
- 数据库迁移必须可审查、可回滚或有明确恢复方案；生产环境不使用 `ddl-auto=update`。
- 先运行目标检查，再按风险扩大验证范围；不得把静态检查描述为端到端验证。

## 工具适配

- 支持 `AGENTS.md` 的工具：直接使用根目录入口。
- 只识别专有规则文件的工具：创建一个很薄的适配文件，内容只需指向 `AGENTS.md` 和 `AI.md`，不要复制整套规则。
- 不支持自动规则加载的工具：在项目配置中显式附加 `AGENTS.md`。

工具适配文件只负责引流，规则事实仍以本模板文件为准。
