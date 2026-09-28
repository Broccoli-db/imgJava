# Java 后端规则

适用于 `src/main/java/**`、`src/test/java/**`、`src/main/resources/db/migration/**` 和相关配置、构建脚本。

## 模块与分层

- 遵循项目现有构建体系和包结构，不混用 Maven 与 Gradle，不在无关任务中迁移构建工具。
- Controller 层负责协议转换和响应映射，Service 层负责业务规则，Repository 层负责持久化。
- Repository、缓存或 ORM 实体不承担跨领域编排；跨模块依赖通过明确接口。
- 避免包级导入环和循环依赖；组件创建、配置绑定、连接与监听由容器和入口显式编排。
- 不在静态初始化块、字段初始化或构造函数中执行 I/O、网络或阻塞操作。

## API

- 使用 Jakarta Bean Validation 在 Controller 边界验证所有外部输入；内部类型不能代替运行时校验。
- 按契约返回稳定的状态码和错误结构；列表接口提供有界分页；排序和过滤字段采用 allowlist。
- 写接口考虑幂等键、唯一约束和并发冲突（乐观锁或条件更新）。
- 文件上传限制大小、数量、类型和存储路径，不信任原始文件名。
- 不让持久化实体直接作为对外 DTO，避免响应结构随实体字段变化而漂移。

## 错误与日志

- 统一异常类型和 `@RestControllerAdvice`，不在每层重复记录同一异常。
- 局部 catch 必须恢复、转换、补充上下文或清理后重新抛出，禁止空 catch 和 `catch (Exception e) {}`。
- 对外错误不暴露堆栈、SQL、文件路径、依赖地址或内部异常消息；使用稳定错误结构（如 `ProblemDetail`）。
- SLF4J 日志包含 request/correlation id（MDC）；敏感字段统一脱敏。
- 使用参数化占位符（`log.info("id={}", id)`），不做字符串拼接，避免无谓的字符串构造和信息泄漏。

## 并发与进程

- 异步结果的异常分支必须处理：`CompletableFuture` 不留无人处理的异常，`@Async` 返回可观测的 `Future`/`CompletableFuture`。
- 不吞 `InterruptedException`；捕获后要么继续抛出，要么 `Thread.currentThread().interrupt()` 恢复中断状态。
- 外部调用设置连接与读超时；支持取消时响应中断。
- 并行批处理设置并发上限（信号量、有界线程池或结构化并发），不要对不受控集合无界并行。
- 重试仅用于幂等且可重试操作，设置最大次数、退避和抖动。
- 线程池和连接池显式配置边界与队列策略，不使用无界队列。
- 共享可变状态使用正确的可见性或同步机制，不用未清理的 `ThreadLocal` 承载跨虚拟线程的上下文。
- 健康检查区分进程存活与依赖就绪。
- 优雅关闭：停止接收新流量、有界等待在途请求、关闭消费者和连接池。

## 数据与安全

- SQL 参数化；动态表名、列名和排序字段使用 allowlist，禁止拼接用户输入。
- 生产环境不使用 `ddl-auto=update`；schema 变更走迁移工具，且可回滚或有明确恢复方案。
- 密码使用项目认可的 `PasswordEncoder`（如 BCrypt/Argon2）与参数，敏感比较使用定时安全比较。
- JWT 明确允许算法并验证有效期及项目要求的 issuer/audience；密钥不设代码默认值。
- Session cookie 使用合适的 `HttpOnly`、`Secure`、`SameSite`；CORS 使用明确来源；反向代理环境明确 `server.forward-headers-strategy` 策略。
- 用户身份、租户、角色和资源所有权都必须在服务端验证，不信任客户端传入的 id、role 或 tenantId。
- 事务边界明确：事务内避免远程调用和长耗时操作；只读查询考虑只读事务。
