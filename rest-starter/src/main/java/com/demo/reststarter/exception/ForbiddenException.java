package com.demo.reststarter.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends RestException {

    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN);
    }

    public ForbiddenException(String messageId) {
        super(HttpStatus.FORBIDDEN, messageId);
    }

    public ForbiddenException(String messageId, String... args) {
        super(HttpStatus.FORBIDDEN, messageId, args);
    }

    public ForbiddenException(String messageId, Throwable throwable, String... args) {
        super(HttpStatus.FORBIDDEN, messageId, throwable, args);
    }
}
