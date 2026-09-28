package com.imgjava.demo.web;

import com.imgjava.demo.domain.DemoNote;

/** 演示用响应 DTO：对外结构不随实体字段变化而漂移。 */
public record DemoNoteResponse(Long id, String title, String content) {

    public static DemoNoteResponse from(DemoNote note) {
        return new DemoNoteResponse(note.getId(), note.getTitle(), note.getContent());
    }
}
