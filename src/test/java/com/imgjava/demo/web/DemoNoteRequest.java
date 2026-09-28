package com.imgjava.demo.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 演示用请求 DTO。校验注解在 Controller 边界生效。 */
public record DemoNoteRequest(

        @NotBlank(message = "title must not be blank")
        @Size(max = 200, message = "title must not exceed 200 characters")
        String title,

        @NotBlank(message = "content must not be blank")
        String content) {
}
