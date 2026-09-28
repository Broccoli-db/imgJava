package com.imgjava.archfixture.repository;

/**
 * 故意违规的 fixture 的一部分，<strong>仅供 ArchitectureTest 自检使用</strong>。
 *
 * <p>它不属于演示切片，也不属于生产代码；存在的唯一目的是被
 * {@code FixtureController} 违规依赖，用来证明分层规则真的会拦。
 */
public class FixtureRepository {

    public String load() {
        return "fixture";
    }
}
