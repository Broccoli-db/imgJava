package com.imgjava.demo.exception;

/** 演示用业务异常：请求的记录不存在。 */
public class DemoNoteNotFoundException extends RuntimeException {

    private final long id;

    public DemoNoteNotFoundException(long id) {
        super("Demo note %d not found".formatted(id));
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
