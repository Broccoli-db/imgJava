# PROJECT - imgJava

## 目标

图床 / 图片存储服务：提供图片上传、存储与访问能力。

[待确认]：目标用户、核心业务范围（是否含缩略图、格式转换、鉴权访问、配额）、服务边界（单体还是拆分为上传与访问两个服务）和当前阶段目标。项目当前只有 Spring Initializr 骨架，无业务代码，本文件不臆造这些内容。

## 技术栈

- JDK 25 + Maven Wrapper（Maven 3.9.16）
- Spring Boot 4.1.1
- PostgreSQL 17 + MongoDB 8 + Redis 6
- Flyway 迁移，JUnit 6 + Testcontainers 2 + ArchUnit 测试，JaCoCo 覆盖率报告

| 维度 | 选型 |
|---|---|
| JDK | 25 |
| 构建工具 | Maven（Wrapper 3.3.4 / Maven 3.9.16） |
| 框架 | Spring Boot 4.1.1（`spring-boot-starter-webmvc`） |
| 数据库/持久化 | PostgreSQL 17（JPA/Hibernate）+ MongoDB 8 + Redis 6 |
| 数据库迁移 | Flyway |
| 校验方式 | Jakarta Bean Validation |
| 测试 | JUnit 6 + Spring Boot Test + Testcontainers 2.0.5 + ArchUnit 1.4.1 |
| 覆盖率 | JaCoCo 0.8.15（只出报告，不设门槛） |
| 运行形态 | 可执行 JAR（spring-boot-maven-plugin） |

## 目录结构

```text
src/main/java/com/imgjava/
  ImgJavaApplication.java          # 应用入口
  config/ web/ service/            # 分层包，目前只有 package-info.java
  repository/ domain/ exception/
  shared/
src/main/resources/
  application.yml                        # 全部配置，尚未按 profile 拆分（YAML 才能写中文）
  db/migration/V1__baseline.sql          # 生产迁移，当前只有基线
src/test/java/com/imgjava/
  TestcontainersConfiguration.java       # PostgreSQL/MongoDB/Redis 容器（测试共享）
  ImgJavaApplicationTests.java           # 上下文加载测试
  TestImgJavaApplication.java            # 用容器启动应用的本地入口
  architecture/ArchitectureTest.java     # 分层约束（ArchUnit）
  archfixture/                           # 故意违规的 fixture，仅供规则自检
  demo/                                  # 演示切片（非业务模型）
src/test/resources/db/migration/         # V9000__create_demo_note.sql：演示表，不进生产产物
```

分层包已建立但只有 `package-info.java`，业务代码尚未落地，详见 `docs/开发规范.md` 第 2 节。
`com.imgjava.demo` 是位于测试源码树的可运行演示切片，真实领域模型落地时整体删除即可。

## 质量闸门

- 编译、静态检查、格式化和测试命令以 `pom.xml` 实际配置为准，不声称运行不存在的任务。
- API 改动验证成功、验证失败、认证失败、授权失败和依赖失败路径。
- 数据库改动包含迁移、约束、事务和回滚/恢复说明。
- `git diff --check` 通过，未包含凭据、生成物或无关改动。当前尚未初始化 git 仓库，该检查暂不可用。
- `docs/记录.md` 已记录实际改动和未验证边界。

## 已知限制

- 无业务代码：生产代码只有 `ImgJavaApplication` 和 7 个只含 `package-info.java` 的分层包，没有任何 Controller、Service、Repository 或实体。
- 分层已建立、约束已就位，但尚无可被约束的生产代码：ArchUnit 规则目前是棘轮，要等业务类落地才会真正拦人。
- 覆盖率报告当前基本为空：JaCoCo 已配置且报告正常生成，但只收录 `ImgJavaApplication` 一个类、覆盖率 0%——JaCoCo 默认只统计 `target/classes`，而演示切片位于 `target/test-classes`。要等生产代码落地才有参考价值。
- 未被版本控制：未初始化 git 仓库，无法用 diff 复核改动来源。
- 无 CI：没有任何流水线配置，验证全靠本地手动执行。
- 无 Actuator、无 Spring Security、无对象存储客户端：规则中涉及健康检查、JWT/Session、上传策略的条目属于目标标准，不是当前实现。
- 配置未分层：`application.properties` 承载全部配置，本地凭据与默认值混在一起。
- 三套存储的职责边界未定义（PostgreSQL / MongoDB / Redis 各自存什么）[待确认]。
