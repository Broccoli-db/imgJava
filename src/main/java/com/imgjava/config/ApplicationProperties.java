package com.imgjava.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 应用自身的描述信息，绑定 {@code imgjava.*}。
 *
 * <p>用构造函数绑定（record）+ {@code @Validated}，因此缺项或空白会在启动时直接失败，
 * 而不是等到第一次读取才暴露。业务代码通过注入本类取值，不散落使用 {@code @Value}
 * 或 {@code Environment.getProperty}。
 *
 * <p>由 {@code @ConfigurationPropertiesScan} 在 {@code ImgJavaApplication} 上注册。
 */
@Validated
@ConfigurationProperties(prefix = "imgjava")
public record ApplicationProperties(

        @NotBlank
        String name,

        @NotBlank
        String description) {
}
