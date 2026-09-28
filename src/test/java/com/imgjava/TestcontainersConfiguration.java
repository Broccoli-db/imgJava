package com.imgjava;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 测试用依赖容器，供各测试类通过 {@code @Import} 复用。
 *
 * <p>镜像版本刻意与 {@code docker-compose.yaml} 保持一致（postgres:17 / mongo:8 / redis:6）。
 * 此前统一用 {@code latest}，导致测试实际跑在 PostgreSQL 18.6 上、与本地开发环境版本不同——
 * 那样测试可能在某一个大版本上通过而本地行为不同。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:8"));
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:6")).withExposedPorts(6379);
    }
}
