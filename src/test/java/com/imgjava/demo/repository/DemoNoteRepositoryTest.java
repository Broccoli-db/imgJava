package com.imgjava.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.imgjava.TestcontainersConfiguration;
import com.imgjava.demo.domain.DemoNote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示用持久化测试，跑在真实的 PostgreSQL 容器上。
 *
 * <p>这里用 {@code @SpringBootTest} 而不是 {@code @DataJpaTest} 切片，是有意的：
 * 本用例要验证的正是"Flyway 能建出表、JPA 映射与之一致"这条完整启动链路，
 * 需要走真实的自动配置路径。切片测试会绕过其中一部分。
 *
 * <p>演示表的建表脚本在 {@code src/test/resources/db/migration/V9000__create_demo_note.sql}，
 * 与生产迁移共用一个 Flyway location，因此不需要在这里额外声明 locations。
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DemoNoteRepositoryTest {

    @Autowired
    private DemoNoteRepository repository;

    @Test
    @DisplayName("迁移建出的表可写入并读回")
    void savesAndReadsBack() {
        DemoNote saved = repository.save(new DemoNote("标题", "内容"));

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId()))
                .isPresent()
                .get()
                .satisfies(note -> {
                    assertThat(note.getTitle()).isEqualTo("标题");
                    assertThat(note.getContent()).isEqualTo("内容");
                });
    }

    @Test
    @DisplayName("分页查询按指定字段排序")
    void findAllIsPagedAndSorted() {
        repository.save(new DemoNote("b", "2"));
        repository.save(new DemoNote("a", "1"));

        Page<DemoNote> page = repository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title")));

        assertThat(page.getContent()).extracting(DemoNote::getTitle).containsExactly("a", "b");
    }
}
