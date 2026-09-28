package com.imgjava.demo.web;

import com.imgjava.demo.domain.DemoNote;
import com.imgjava.demo.service.DemoNoteService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演示用 Controller，见 {@link com.imgjava.demo.domain.DemoNote}。
 *
 * <p>只做协议转换：解析输入、调用 service、映射响应。不写业务规则，不碰 repository。
 * 路径带 {@code /demo} 前缀，明确它不是真实业务接口。
 */
@RestController
@RequestMapping("/api/demo/notes")
public class DemoNoteController {

    private final DemoNoteService service;

    public DemoNoteController(DemoNoteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DemoNoteResponse create(@Valid @RequestBody DemoNoteRequest request) {
        DemoNote note = service.create(request.title(), request.content());
        return DemoNoteResponse.from(note);
    }

    @GetMapping("/{id}")
    public DemoNoteResponse get(@PathVariable long id) {
        return DemoNoteResponse.from(service.get(id));
    }

    @GetMapping
    public List<DemoNoteResponse> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String sortBy) {
        return service.list(page, size, sortBy).stream()
                .map(DemoNoteResponse::from)
                .toList();
    }
}
