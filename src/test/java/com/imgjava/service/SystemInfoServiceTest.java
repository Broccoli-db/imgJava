package com.imgjava.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.imgjava.config.ApplicationProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * 纯单元测试：不启动 Spring 上下文，依赖全部手工注入。
 *
 * <p>时间用 {@code Clock.fixed} 固定，所以可以对 serverTime 做等值断言，
 * 不必放宽成「非空」或「接近现在」这类弱断言。
 */
class SystemInfoServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-09-28T04:05:06Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Test
    @DisplayName("返回绑定的应用信息与注入时钟的时间")
    void returnsBoundPropertiesAndInjectedTime() {
        ApplicationProperties properties = new ApplicationProperties("imgJava", "图床 / 图片存储服务");
        SystemInfoService service = new SystemInfoService(properties, new MockEnvironment(), FIXED_CLOCK);

        SystemInfo info = service.current();

        assertThat(info.application()).isEqualTo("imgJava");
        assertThat(info.description()).isEqualTo("图床 / 图片存储服务");
        assertThat(info.serverTime()).isEqualTo(FIXED_NOW);
        assertThat(info.activeProfiles()).isEmpty();
        assertThat(info.javaVersion()).isNotBlank();
    }

    @Test
    @DisplayName("激活的 profile 会被带出")
    void includesActiveProfiles() {
        ApplicationProperties properties = new ApplicationProperties("imgJava", "图床 / 图片存储服务");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        SystemInfoService service = new SystemInfoService(properties, environment, FIXED_CLOCK);

        assertThat(service.current().activeProfiles()).containsExactly("local");
    }
}
