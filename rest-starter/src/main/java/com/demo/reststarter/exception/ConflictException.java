package com.demo.reststarter.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends RestException {
  public ConflictException() {
    super(HttpStatus.CONFLICT);
  }

  public ConflictException(String messageId) {
    super(HttpStatus.CONFLICT, messageId);
  }

  public ConflictException(String messageId, String... args) {
    super(HttpStatus.CONFLICT, messageId, args);
  }

  public ConflictException(String messageId, Throwable throwable, String... args) {
    super(HttpStatus.CONFLICT, messageId, throwable, args);
  }
}
