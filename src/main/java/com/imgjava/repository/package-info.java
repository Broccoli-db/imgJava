/**
 * 数据访问层：持久化读写。
 *
 * <p>只负责存取，不承载业务编排；跨模块组合是 service 的职责。
 *
 * <p>约束（由 {@code com.imgjava.architecture.ArchitectureTest} 强制）：
 * <ul>
 *   <li>不得依赖 {@code web} 或 {@code service} 包</li>
 *   <li>本包中的接口必须以 {@code Repository} 结尾</li>
 * </ul>
 */
package com.imgjava.repository;
