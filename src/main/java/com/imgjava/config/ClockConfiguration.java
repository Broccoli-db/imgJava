package com.imgjava.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 提供统一的 {@link Clock}。
 *
 * <p>业务代码取当前时间一律注入 {@code Clock}，不直接调用 {@code Instant.now()}。
 * 这样测试可以注入 {@code Clock.fixed(...)} 得到确定的时间，不需要放宽断言或依赖
 * 真实时钟——对应 {@code .ai/rules/testing.md} 中「时间通过可控边界注入」的要求。
 */
@Configuration
public class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
