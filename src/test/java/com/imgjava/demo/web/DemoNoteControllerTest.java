package com.imgjava.demo.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgjava.demo.domain.DemoNote;
import com.imgjava.demo.exception.DemoNoteNotFoundException;
import com.imgjava.demo.service.DemoNoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 演示用 Web 层切片测试：只加载 Web 层，Service 用 mock 替换，不碰数据库。
 *
 * <p>注意 {@code @WebMvcTest} 在 Spring Boot 4 中的位置是
 * {@code org.springframework.boot.webmvc.test.autoconfigure}，
 * 与 Boot 3 的 {@code org.springframework.boot.test.autoconfigure.web.servlet} 不同。
 */
@WebMvcTest(DemoNoteController.class)
class DemoNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DemoNoteService service;

    @Test
    @DisplayName("POST 成功返回 201 与创建结果")
    void createReturns201() throws Exception {
        DemoNote note = new DemoNote("t", "c");
        note.setId(1L);
        when(service.create("t", "c")).thenReturn(note);

        mockMvc.perform(post("/api/demo/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("t"))
                .andExpect(jsonPath("$.content").value("c"));
    }

    @Test
    @DisplayName("POST 标题为空白返回 400")
    void createRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/demo/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"content\":\"c\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST 缺少 content 返回 400")
    void createRejectsMissingContent() throws Exception {
        mockMvc.perform(post("/api/demo/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET 不存在的记录返回 404 与 ProblemDetail")
    void getReturns404WhenMissing() throws Exception {
        when(service.get(anyLong())).thenThrow(new DemoNoteNotFoundException(42L));

        mockMvc.perform(get("/api/demo/notes/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Demo note not found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
