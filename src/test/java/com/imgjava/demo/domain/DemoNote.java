package com.imgjava.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 演示用实体——<strong>不是业务模型</strong>。
 *
 * <p>它存在的唯一目的，是让分层约定、ArchUnit 规则、JaCoCo 和 Flyway 都拥有一份
 * 可运行的真实代码。刻意命名为 {@code DemoNote} 而不是 {@code Image}，以免替真实
 * 业务提前决定"图片元数据存哪套存储"。
 *
 * <p>位于测试源码树，因此不会进入生产产物；对应的建表脚本也在
 * {@code src/test/resources/db/migration-demo/}，不在生产的迁移目录里。
 * 真实领域模型落地时，整个 {@code com.imgjava.demo} 包连同测试资源一起删除即可。
 */
@Entity
@Table(name = "demo_note")
@Getter
@Setter
@NoArgsConstructor
public class DemoNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String content;

    public DemoNote(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
