/**
 * 业务用例层：编排业务规则与事务边界。
 *
 * <p>对外输入在此完成业务级校验；事务边界定义在这一层，而不是 Controller 或 Repository。
 *
 * <p>约束（由 {@code com.imgjava.architecture.ArchitectureTest} 强制）：
 * <ul>
 *   <li>不得依赖 {@code web} 包，不拼装 HTTP 响应</li>
 *   <li>只能被 {@code web} 依赖，不允许被 {@code repository}、{@code domain} 反向依赖</li>
 * </ul>
 */
package com.imgjava.service;
