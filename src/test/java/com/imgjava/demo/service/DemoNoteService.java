package com.imgjava.demo.service;

import com.imgjava.demo.domain.DemoNote;
import com.imgjava.demo.exception.DemoNoteNotFoundException;
import com.imgjava.demo.repository.DemoNoteRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 演示用业务服务，见 {@link com.imgjava.demo.domain.DemoNote}。
 *
 * <p>它示范三件事：事务边界落在 service 层；分页有硬上限；排序字段走 allowlist
 * 而不是把用户输入直接拼进查询。
 */
@Service
public class DemoNoteService {

    /** 单页最大条数，超过则截断，避免调用方用 size=100000 拖垮数据库。 */
    public static final int MAX_PAGE_SIZE = 50;

    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 允许排序的字段白名单；其余一律回退到默认值。 */
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "title");

    private final DemoNoteRepository repository;

    public DemoNoteService(DemoNoteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DemoNote create(String title, String content) {
        return repository.save(new DemoNote(title, content));
    }

    @Transactional(readOnly = true)
    public DemoNote get(long id) {
        return repository.findById(id).orElseThrow(() -> new DemoNoteNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<DemoNote> list(int page, int size, String sortBy) {
        String sortField = sortBy != null && SORTABLE_FIELDS.contains(sortBy) ? sortBy : "id";
        int pageNumber = Math.max(page, 0);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(sortField));
        return repository.findAll(pageable);
    }
}
