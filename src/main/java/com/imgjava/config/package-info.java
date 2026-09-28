/**
 * 配置装配层：配置绑定类与容器装配。
 *
 * <p>把外部配置集中绑定成类型安全的配置对象并在启动时校验；业务代码不得散落读取环境变量
 * 或零散使用 {@code @Value}。
 *
 * <p>约束（由 {@code com.imgjava.architecture.ArchitectureTest} 强制）：
 * <ul>
 *   <li>不使用 {@code System.out}、{@code printStackTrace}、{@code java.util.logging}</li>
 * </ul>
 */
package com.imgjava.config;
