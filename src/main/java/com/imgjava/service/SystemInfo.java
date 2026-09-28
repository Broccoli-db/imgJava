package com.imgjava.service;

import java.time.Instant;
import java.util.List;

/**
 * {@link SystemInfoService} 的输出契约。
 *
 * <p>定义在 service 包而不是 web 包：service 不允许依赖 web（由 ArchUnit 强制），
 * 所以它的返回值必须是 web 之外的类型。web 层按需映射成自己的响应结构。
 */
public record SystemInfo(
        String application,
        String description,
        String javaVersion,
        List<String> activeProfiles,
        Instant serverTime) {
}
