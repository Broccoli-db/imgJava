# ARCHITECTURE - 稳定架构知识

## 运行时

- JDK：25
- 构建工具：Maven（Wrapper 3.3.4 / Maven 3.9.16）
- 框架：Spring Boot 4.1.1（`spring-boot-starter-webmvc`）
- 运行形态：可执行 JAR（spring-boot-maven-plugin）

## 应用分层

```text
src/main/java/com/imgjava/
  ImgJavaApplication.java          # 应用入口
  config/ web/ service/            # 分层包：目前只有 package-info.java 声明职责，
  repository/ domain/ exception/   # 没有任何业务类
  shared/
src/main/resources/
  application.yml                        # 全部配置，尚未按 profile 拆分
  db/migration/V1__baseline.sql          # 生产迁移，当前只有基线
src/test/java/com/imgjava/
  TestcontainersConfiguration.java       # PostgreSQL/MongoDB/Redis 容器（测试共享）
  ImgJavaApplicationTests.java           # 上下文加载测试
  TestImgJavaApplication.java            # 用容器启动应用的本地入口
  architecture/ArchitectureTest.java     # 分层约束（ArchUnit）
  archfixture/                           # 故意违规的 fixture，仅供规则自检
  demo/                                  # 演示切片，见下
src/test/resources/db/migration/         # V9000__create_demo_note.sql：演示表，
                                         # 与生产迁移共用 Flyway location 但不进生产产物
```

生产代码现状：7 个分层包已建立，但只有 `package-info.java`，没有 Controller、Service、Repository 或实体。因此 `.ai/ARCHITECTURE.md` 的分层约束目前是**棘轮**——拦不下现存代码，但会拦住未来违反分层的改动。

演示切片 `com.imgjava.demo`（位于 `src/test`，**不是业务模型**）：
`domain/DemoNote` → `repository/DemoNoteRepository` → `service/DemoNoteService` → `web/DemoNoteController`，
外加 `exception/DemoNoteExceptionHandler`，接口在 `/api/demo/notes`。

它存在的唯一目的，是让分层规则、ArchUnit、JaCoCo 和 Flyway 都有一份可运行的真实代码。刻意叫 `DemoNote` 而不是 `Image`，以免替真实业务提前决定"图片元数据存哪套存储"。

**演示表为什么放在默认迁移位置而不是独立目录**：`DemoNote` 是测试源码里的 JPA 实体，会被 Hibernate 的实体扫描全局捕获，于是每一个 `@SpringBootTest` 上下文都会校验这张表存不存在。把它放进 `classpath:db/migration`（Flyway 会跨 classpath 合并同一 location），所有测试自动拿到这张表；若改用独立 location，则每个测试类都得各自声明 `spring.flyway.locations`，漏一个就报 `Schema validation: missing table`。版本号取 9000 是为了永久规避与真实迁移的编号冲突。生产打包时测试资源不参与，这张表不会进入生产迁移历史。

真实领域模型落地时，整个 `com.imgjava.demo` 包连同 `V9000__create_demo_note.sql` 一起删除即可，不需要补删除迁移。

## 数据与集成点

| 集成点 | 契约来源 | 所有者 | 失败策略 |
|---|---|---|---|
| PostgreSQL 17 | JPA 实体 + `db/migration/` 下的 Flyway 脚本 | [待确认] | [待确认] |
| MongoDB 8 | [待确认]：集合与文档结构尚未定义 | [待确认] | [待确认] |
| Redis 6 | [待确认]：缓存键与过期策略尚未定义 | [待确认] | [待确认] |

三套存储的职责边界（元数据、图片内容、缓存各自落在哪里）尚未确定，是当前最大的架构空缺。边界确定前不要新增实体或集合。

## 配置

现状：全部配置集中在 `src/main/resources/application.yml`，没有 profile 拆分。

**格式是 YAML 而不是 properties**，这是有原因的：Spring Boot 4 的 `OriginTrackedPropertiesLoader` 在未显式传入编码时硬编码退到 ISO-8859-1（`spring-boot-4.1.1-sources.jar` 中该文件第 211 行），因此 `.properties` 里的中文必然乱码。YAML 由 SnakeYAML 按 UTF-8 读取，中文可以直接写。**新增配置一律写进 `application.yml`，不要再建 `.properties` 文件。**

配置绑定类：`com.imgjava.config.ApplicationProperties`（`@ConfigurationProperties(prefix = "imgjava")`，record 构造绑定 + `@Validated`，启动时校验缺项）。由 `ImgJavaApplication` 上的 `@ConfigurationPropertiesScan` 注册。

已使用的属性名（只记录名称，层级用 `.` 表示）：

- 应用：`spring.application.name`
- 服务：`server.port`（本机 8080 已被其他项目占用，设为 8082）
- 应用信息：`imgjava.name`、`imgjava.description`（绑定到 `ApplicationProperties`）
- 接口文档：`springdoc.swagger-ui.doc-expansion`、`springdoc.swagger-ui.operations-sorter`、`springdoc.swagger-ui.tags-sorter`
- Redis：`spring.data.redis.host`、`spring.data.redis.port`、`spring.data.redis.password`
- 数据源：`spring.datasource.driver-class-name`、`spring.datasource.url`、`spring.datasource.username`、`spring.datasource.password`
- JPA：`spring.jpa.hibernate.ddl-auto`、`spring.jpa.show-sql`、`spring.jpa.properties.hibernate.format_sql`
- MongoDB：`spring.data.mongodb.host`、`spring.data.mongodb.port`、`spring.data.mongodb.authentication-database`、`spring.data.mongodb.username`、`spring.data.mongodb.password`、`spring.data.mongodb.database`
- Flyway：`spring.flyway.enabled`

待办：本地凭据与默认值混在同一文件，引入真实环境前必须拆分为本地 profile 或环境变量。

## 数据库与迁移

- PostgreSQL 17：Spring Data JPA / Hibernate 访问。
- 迁移：Flyway，脚本位于 `src/main/resources/db/migration/`，命名 `V<版本>__<描述>.sql`。
- Flyway 依赖必须使用 `spring-boot-starter-flyway`（Boot 4 的 starter），仅声明 `org.flywaydb:flyway-core` 不会启用自动配置；PostgreSQL 方言另需 `org.flywaydb:flyway-database-postgresql`。两者版本均由 Spring Boot BOM 托管。
- `spring.jpa.hibernate.ddl-auto=validate`：Hibernate 只校验实体与表是否匹配，不再改表；结构变更一律走迁移脚本。
- MongoDB 8：Spring Data MongoDB，当前没有实体或集合定义。
- Redis 6：Spring Data Redis，当前没有缓存定义。
- 测试：`TestcontainersConfiguration` 为 PostgreSQL、MongoDB、Redis 各起一个容器，通过 `@ServiceConnection` 注入连接信息；测试不依赖本地 docker compose 实例。

## 已知坑

| 现象 | 原因 | 规避方式 |
|---|---|---|
| 按记忆或旧教程写的 starter 坐标在本项目不存在 | Spring Boot 4 调整了 starter 命名：Web 由 `spring-boot-starter-web` 变为 `spring-boot-starter-webmvc`，测试侧按模块拆分（如 `spring-boot-starter-data-jpa-test`、`spring-boot-starter-webmvc-test`）。Boot 3 时代的资料不能直接照抄 | 依赖坐标以本项目 `pom.xml` 实际内容为准 |
| 加了迁移依赖但迁移完全不执行，且没有任何报错 | Spring Boot 4 把自动配置按技术拆成独立模块：只声明 `org.flywaydb:flyway-core` 拿到的是 Flyway 库本体，不含自动配置；`spring-boot-autoconfigure` 4.1.1 中已无任何 Flyway 类，因此启动时既不迁移也不报错，静默失效 | 使用 `spring-boot-starter-flyway`（带入 `spring-boot-flyway` 自动配置模块）；PostgreSQL 方言另需 `flyway-database-postgresql`。BOM 中同类模块还有 `spring-boot-jpa`、`spring-boot-mongodb`、`spring-boot-data-redis` 等，判断依据是启动日志有无对应组件输出 |
| 按 Boot 3 的位置 import 测试注解会编译失败 | Spring Boot 4 把测试切片注解也移进了按技术拆分的模块，包名与 Boot 3 不同 | 实测两处：`@WebMvcTest` 在 `org.springframework.boot.webmvc.test.autoconfigure`（Boot 3 是 `...test.autoconfigure.web.servlet`）；`@DataJpaTest` 在 `org.springframework.boot.data.jpa.test.autoconfigure`（Boot 3 是 `...test.autoconfigure.orm.jpa`）。不确定时直接在对应的 `spring-boot-*-test` jar 里查全限定名 |
| 测试容器与本地 compose 的依赖版本不一致 | `TestcontainersConfiguration` 原先统一用 `mongo:latest`/`postgres:latest`/`redis:latest`，与 `docker-compose.yaml` 固定的版本不同 | 已改为与 compose 一致的 `mongo:8`/`postgres:17`/`redis:6`；新增容器时保持这条约定 |
| ArchUnit 的 `areInterfaces()` 会把 `package-info` 当成接口 | `package-info.java` 被 javac 编译为 `ACC_INTERFACE \| ACC_ABSTRACT \| ACC_SYNTHETIC` 的合成接口（`javap -v` 可验证），因此凡是按接口筛选的规则都会命中它 | 在按接口筛选的规则上排除 `package-info`：`.and().haveSimpleNameNotEndingWith("package-info")`。ArchUnit 的 `classes().that()` 规则遇到零匹配默认判定为错误，生产包当前为空时需 `.allowEmptyShould(true)` |
| 测试源码里的 JPA 实体影响所有 Spring 上下文 | Hibernate 的实体扫描覆盖整个 classpath，测试类路径上的 `@Entity` 会进入每个 `@SpringBootTest` 的持久化单元；`ddl-auto=validate` 随即要求这张表在所有测试里都存在 | 演示/辅助实体若必须在测试源码里，把建表脚本放进 `src/test/resources/db/migration/` 与生产迁移共用 Flyway location，让所有测试自动获得该表 |
| `.properties` 里的中文必然是乱码，且没有任何报错 | Spring Boot 4 的 `PropertiesPropertySourceLoader.load()` 传入的编码是 `null`，`OriginTrackedPropertiesLoader` 随即退到 `StandardCharsets.ISO_8859_1`（源码第 211 行实测确认）。文件字节是正确的 UTF-8，坏在解码环节，表现是 `图床` 变成 `å¾åº` | 配置一律写进 `application.yml`（SnakeYAML 按 UTF-8 读），不要新建 `.properties`。若必须用 properties，非 ASCII 只能写 `\uXXXX` 转义 |
| 改配置文件名后新配置不生效，旧值仍然在用 | Maven 的 `process-resources` 只拷贝、从不删除，`target/classes` 里会残留改名前的旧文件；且 `application.properties` 的优先级高于 `application.yml`，于是被删掉的旧文件继续覆盖新文件。本次实测就因此误判为"YAML 也乱码" | 改配置文件名或删除配置文件后执行 `./mvnw clean`，或手动确认 `target/classes` 下只剩预期的那一个文件 |
| 首次执行 `./mvnw test` 耗时很长（本机实测首次约 58 秒，镜像就绪后约 6 秒） | Testcontainers 需要拉取并启动 PostgreSQL/MongoDB/Redis 容器 | 属正常现象，镜像拉取后变快；不要因此误判为测试失败或调大超时 |
| 测试通过与本地运行行为不一致 | 已确认：测试容器使用 `postgres:latest`（实测 PostgreSQL 18.6），而 `docker-compose.yaml` 固定 `postgres:17`；MongoDB、Redis 同样存在 `latest` 与固定版本的差异 | 让测试与本地使用同一组版本标签，避免测试在某一大版本上通过而本地行为不同 |
| 有人为图方便把 `ddl-auto` 改回 `update` | 让 Hibernate 自动建表比写迁移脚本快 | 保持 `validate`；结构变更一律新增 Flyway 迁移脚本并纳入版本控制 |
| 启动日志出现 `spring.jpa.open-in-view is enabled by default` 警告 | Spring Boot 默认开启 Open Session in View，懒加载可能在视图渲染阶段触发查询，掩盖 N+1 和连接占用问题 | 按需显式设置 `spring.jpa.open-in-view=false`；本项目当前未设置 |
| 虚拟线程下请求上下文或日志 MDC 丢失、串号 | `ThreadLocal` 的生命周期与虚拟线程调度不一致 | 显式传递上下文，不依赖未清理的 `ThreadLocal`；必要时使用作用域值 |
| 异步方法抛出的异常无声消失 | `CompletableFuture` 或 `@Async` 的异常分支未处理 | 明确处理 `exceptionally`/`handle`，或配置统一的异步异常处理器 |

## 常用命令

```bash
docker compose up -d                     # 启动本地 PostgreSQL / MongoDB / Redis
./mvnw spring-boot:run                   # 启动应用
./mvnw test                              # 运行测试（Testcontainers 会另起容器）
./mvnw -Dtest=ImgJavaApplicationTests test   # 只跑指定测试
```

Windows 原生终端使用 `mvnw.cmd` 替代 `./mvnw`。
