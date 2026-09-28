package com.imgjava.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imgjava.demo.domain.DemoNote;
import com.imgjava.demo.exception.DemoNoteNotFoundException;
import com.imgjava.demo.repository.DemoNoteRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** 演示用服务层单元测试：只测业务规则，不碰 Spring 上下文和数据库。 */
@ExtendWith(MockitoExtension.class)
class DemoNoteServiceTest {

    @Mock
    private DemoNoteRepository repository;

    @InjectMocks
    private DemoNoteService service;

    @Test
    @DisplayName("create 保存并返回实体")
    void createSavesNote() {
        when(repository.save(any(DemoNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DemoNote saved = service.create("标题", "内容");

        assertThat(saved.getTitle()).isEqualTo("标题");
        assertThat(saved.getContent()).isEqualTo("内容");
    }

    @Test
    @DisplayName("get 在记录不存在时抛出 DemoNoteNotFoundException")
    void getThrowsWhenMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(7L))
                .isInstanceOf(DemoNoteNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    @DisplayName("list 把不在白名单的排序字段回退到 id")
    void listFallsBackToDefaultSortForUnknownField() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list(0, 20, "content; DROP TABLE demo_note");

        assertThat(capturedPageable().getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    @DisplayName("list 接受白名单内的排序字段")
    void listAcceptsAllowedSortField() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list(0, 20, "title");

        assertThat(capturedPageable().getSort()).isEqualTo(Sort.by("title"));
    }

    @Test
    @DisplayName("list 把超出上限的 size 截断到 MAX_PAGE_SIZE")
    void listClampsPageSize() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list(0, 100_000, "title");

        assertThat(capturedPageable().getPageSize()).isEqualTo(DemoNoteService.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("list 对负数页码回退到第一页")
    void listFallsBackToFirstPageForNegativePage() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.list(-5, 10, null);

        assertThat(capturedPageable().getPageNumber()).isZero();
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        return captor.getValue();
    }
}
