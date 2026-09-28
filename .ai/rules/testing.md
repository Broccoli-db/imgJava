# 测试与验证规则

- 先从 `pom.xml` / `build.gradle` 和现有 CI 确认真实命令，不声称运行不存在的任务或插件。
- 测试层级与风险匹配：纯业务规则用单元测试，Controller 与持久化用切片测试或 Testcontainers 集成测试，关键用户链路再做端到端测试。
- 测试覆盖成功、非法输入、依赖失败、超时、权限、并发和清理路径，而不只覆盖 happy path。
- 测试不得访问生产服务或生产数据；数据库测试使用 Testcontainers、隔离 schema 或事务回滚。
- 时间、随机数和网络通过可控边界注入（如注入 `Clock`、可控随机源、测试替身），避免依赖固定 `Thread.sleep`。
- 异步与并发测试使用确定性同步手段（`CountDownLatch`、`Awaitility` 等），不用无界轮询或长睡眠。
- 不因修复测试而放宽真实约束；测试失败先判断是实现、测试还是环境问题。
- 分层与依赖方向等架构约束可用 ArchUnit 之类的静态测试固化，避免规则只停留在文档。
- 配置或 guard 改动要验证跨平台路径、大小写、分隔符、缺失配置、误拦和绕过用例。

## 项目命令

```bash
./mvnw test                                  # 全部测试（需要 Docker，首次约 1 分钟）
./mvnw -Dtest=ImgJavaApplicationTests test   # 只跑指定测试类
./mvnw -Dtest=ImgJavaApplicationTests#contextLoads test   # 只跑指定方法
```

Windows 原生终端使用 `mvnw.cmd` 替代 `./mvnw`。

当前仅有 `ImgJavaApplicationTests`（上下文加载）一个测试，没有任何业务测试。新增业务代码时必须同步补齐对应层级的测试。
