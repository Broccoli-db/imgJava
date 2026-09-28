/**
 * 共享基础设施：日志、通用工具、横切关注点。
 *
 * <p>放置被多层复用的无状态工具与基础设施封装。不是"什么都能放"的杂物间——
 * 只放确实被多个层共用、且不承载业务规则的东西。
 *
 * <p>约束（由 {@code com.imgjava.architecture.ArchitectureTest} 强制）：
 * <ul>
 *   <li>不使用 {@code System.out}、{@code printStackTrace}、{@code java.util.logging}</li>
 * </ul>
 */
package com.imgjava.shared;
