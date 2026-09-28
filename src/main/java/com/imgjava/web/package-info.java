/**
 * HTTP 层：Controller 与请求/响应 DTO。
 *
 * <p>只做协议转换——解析输入、调用 service、映射响应。不承载业务规则。
 *
 * <p>约束（由 {@code com.imgjava.architecture.ArchitectureTest} 强制）：
 * <ul>
 *   <li>不得依赖 {@code repository} 包，必须经过 {@code service}</li>
 *   <li>不得导入 {@code jakarta.persistence}，持久化实体不作为对外 DTO</li>
 *   <li>本包中以 {@code @RestController} 标注的类必须以 {@code Controller} 结尾</li>
 * </ul>
 */
package com.imgjava.web;
