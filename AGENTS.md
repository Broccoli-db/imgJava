# AGENTS.md - imgJava AI 协作规则

> 这是本项目面向所有 AI 开发辅助工具的统一入口。无论使用 Codex、Claude Code、Cursor、Cline、Copilot、Windsurf 或其他工具，开始工作前都应读取本文件。

## 项目

图床 / 图片存储服务：提供图片上传、存储与访问能力。当前处于骨架阶段，业务代码尚未实现。

[待确认]：目标用户、图片存储介质（对象存储 / 本地文件系统 / 数据库）、访问控制模型、上传来源与配额策略。

## Java 技术栈

- JDK 25，Maven（Wrapper 3.3.4 / Maven 3.9.16）
- Spring Boot 4.1.1（`spring-boot-starter-webmvc`）
- PostgreSQL 17（JPA/Hibernate）+ MongoDB 8 + Redis 6，迁移使用 Flyway
- 测试：JUnit 6 + Spring Boot Test + Testcontainers 2 + ArchUnit；覆盖率用 JaCoCo（只出报告，不卡构建）

## 目录结构

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

## 常用命令

```bash
docker compose up -d                     # 启动本地 PostgreSQL / MongoDB / Redis
./mvnw spring-boot:run                   # 启动应用
./mvnw test                              # 运行测试（Testcontainers 会另起容器）
./mvnw -Dtest=ImgJavaApplicationTests test   # 只跑指定测试
```

Windows 原生终端使用 `mvnw.cmd` 替代 `./mvnw`。

## 规则优先级

1. 平台和系统安全约束。
2. 用户当前明确要求与授权范围。
3. 本项目红线。
4. 团队工程约定。
5. 风格偏好。

用户可以授权执行发布、部署等受控操作，但不得用普通指令绕过凭据保护、越权访问或破坏性安全边界。规则冲突且无法判断时，停止相关操作并询问用户。

## 红线

1. **项目目录之外的一切文件视为只读**：可以读取，但未经用户明确授权，不得创建、修改、删除或重命名。本条优先级高于本文件其余所有规则。
2. 不读取、输出、提交或记录 `application-local.*`、密钥库、token、cookie、私钥和生产凭据；只读取 `application-*.example.*` 等无敏感值模板。
3. 不执行未授权的 `git push --force`、`reset --hard`、删分支、发布、部署、生产数据库变更或生产写操作。
4. 不修改外部只读目录，不覆盖、回退或删除用户已有改动。
5. 不臆造 API、数据库表、消息格式、权限名称或配置项；不确定时标记 `[待确认]`。
6. 所有外部输入都必须在系统边界验证；认证不等于授权。

## Java / Spring Boot 工程约定

1. 先确认 `pom.xml`、构建包装器（`mvnw`）、JDK 版本和依赖管理方式，使用项目现有构建工具，不混用 Maven 与 Gradle。
2. Controller 只处理 HTTP 映射与协议转换；业务逻辑放 service/use-case；数据访问放 repository，禁止跨层复制规则。
3. 异常交给统一异常处理（`@RestControllerAdvice`）；仅在恢复、转换、补充上下文或释放资源时局部捕获，禁止静默 catch。
4. 并发任务使用有界并发；异步结果必须处理异常分支；不吞 `InterruptedException`，不在虚拟线程中依赖未清理的 `ThreadLocal`。
5. 配置集中通过 `@ConfigurationProperties` 读取并在启动时校验；业务代码不得散落读取 `System.getenv` 或零散 `@Value`。
6. SQL 使用参数化查询或 ORM 参数绑定；密码使用可靠的 `PasswordEncoder`；JWT/Session 必须校验算法、时效、受众或签发者等项目要求。
7. 日志使用 SLF4J，不记录凭据、Authorization、cookie、完整请求体或敏感个人数据。
8. 新依赖必须说明必要性，优先复用现有能力，不做无关升级。

## 工作流程

- 开始前：读取本文件、`AI.md`、`.ai/CORE.md`、`.ai/ARCHITECTURE.md`、相关 `.ai/rules/` 和 `docs/记录.md` 顶部条目。
- 工作中：先搜索现有实现和测试；保持改动聚焦；接口或数据契约不明时先核对事实。
- 完成前：按 `.ai/CHECKLIST.md` 执行与风险匹配的检查；修改任务更新 `docs/记录.md`，只读任务不写日志。

## 外部只读边界

无。当前未配置外部只读目录。后续如需把共享库或其他仓库纳入边界，用 `scripts/setup-protected-roots.sh`（POSIX）或 `scripts/setup-protected-roots.ps1`（Windows）配置 `PROTECTED_READONLY_ROOTS`，并按需接入 `.ai/guards/ProtectProjectBoundaries.java`。

## 项目特有约束

- 本地开发依赖 `docker-compose.yaml` 提供的 PostgreSQL 17、MongoDB 8 和 Redis 6；启动应用前先 `docker compose up -d`。
- 三套存储的职责边界尚未定义：PostgreSQL 存什么、MongoDB 存什么、Redis 缓存什么，均为 [待确认]。在边界确定前不要新增实体或集合。
- 本地开发凭据当前直接写在 `src/main/resources/application.properties`（取值与 `docker-compose.yaml` 一致）。这是本地开发约定值而非生产秘密，但引入真实环境前必须拆分为本地 profile 或环境变量。
- 项目尚未初始化 git 仓库，`.ai/CHECKLIST.md` 中依赖 `git diff` 的检查项当前不可用。
- 尚未引入 CI、Actuator、Spring Security 和对象存储客户端，相关规则属于目标标准而非现状。
