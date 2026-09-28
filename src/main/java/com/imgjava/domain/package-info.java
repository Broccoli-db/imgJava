/**
 * 领域层：领域模型与持久化实体。
 *
 * <p>是依赖方向的底端——可以被上层依赖，自己不依赖任何上层。
 *
 * <p>约束（由 {@code com.imgjava.architecture.ArchitectureTest} 强制）：
 * <ul>
 *   <li>不得依赖 {@code web}、{@code service}、{@code repository} 包</li>
 *   <li>不使用 {@code System.out}、{@code printStackTrace}、{@code java.util.logging}</li>
 * </ul>
 */
package com.imgjava.domain;
