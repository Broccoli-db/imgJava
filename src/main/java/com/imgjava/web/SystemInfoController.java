package com.imgjava.web;

import com.imgjava.service.SystemInfo;
import com.imgjava.service.SystemInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 应用自检接口。
 *
 * <p>只做协议转换：调用 service 并把结果返回，不含任何业务规则。
 * 返回的是 service 的输出契约 {@link SystemInfo}，没有额外的响应 DTO——
 * 该记录不含持久化实体，且字段即对外契约，多加一层映射只会带来重复。
 */
@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    private final SystemInfoService service;

    public SystemInfoController(SystemInfoService service) {
        this.service = service;
    }

    @GetMapping("/info")
    public SystemInfo info() {
        return service.current();
    }
}
