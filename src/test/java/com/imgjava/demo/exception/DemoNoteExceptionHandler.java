package com.imgjava.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 演示用统一异常处理。
 *
 * <p>对外只返回稳定结构（RFC 9457 ProblemDetail），不暴露堆栈、SQL 或内部实现；
 * 日志记录留给 service 层，避免同一异常被多层重复记录。
 */
@RestControllerAdvice
public class DemoNoteExceptionHandler {

    @ExceptionHandler(DemoNoteNotFoundException.class)
    ProblemDetail handleNotFound(DemoNoteNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Demo note not found");
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
