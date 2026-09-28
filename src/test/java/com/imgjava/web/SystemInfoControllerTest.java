package com.imgjava.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgjava.service.SystemInfo;
import com.imgjava.service.SystemInfoService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Web 层切片测试：只加载 Web 层，service 用 mock 替换，不启动完整上下文。 */
@WebMvcTest(SystemInfoController.class)
class SystemInfoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SystemInfoService service;

    @Test
    @DisplayName("GET /api/system/info 返回 200 与系统信息")
    void returnsSystemInfo() throws Exception {
        when(service.current()).thenReturn(new SystemInfo(
                "imgJava",
                "图床 / 图片存储服务",
                "25.0.4.1",
                List.of("local"),
                Instant.parse("2026-09-28T04:05:06Z")));

        mockMvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("imgJava"))
                .andExpect(jsonPath("$.description").value("图床 / 图片存储服务"))
                .andExpect(jsonPath("$.javaVersion").value("25.0.4.1"))
                .andExpect(jsonPath("$.activeProfiles[0]").value("local"))
                .andExpect(jsonPath("$.serverTime").value("2026-09-28T04:05:06Z"));
    }
}
