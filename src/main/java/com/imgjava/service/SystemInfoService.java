package com.imgjava.service;

import com.imgjava.config.ApplicationProperties;
import java.time.Clock;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 汇总应用自身的运行信息。
 *
 * <p>依赖都是注入的：配置来自 {@link ApplicationProperties}（启动时已校验），
 * 时间来自 {@link Clock}（测试可固定），激活的 profile 来自 {@link Environment}。
 * 没有任何静态调用或隐式的全局状态，因此可以脱离 Spring 上下文单测。
 */
@Service
public class SystemInfoService {

    private final ApplicationProperties properties;
    private final Environment environment;
    private final Clock clock;

    public SystemInfoService(ApplicationProperties properties, Environment environment, Clock clock) {
        this.properties = properties;
        this.environment = environment;
        this.clock = clock;
    }

    public SystemInfo current() {
        return new SystemInfo(
                properties.name(),
                properties.description(),
                System.getProperty("java.version"),
                List.of(environment.getActiveProfiles()),
                clock.instant());
    }
}
