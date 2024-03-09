package com.demo.awsstorage.exception;

import com.demo.reststarter.exception.ConflictException;

public class FileStorageConflictException extends ConflictException {

    public FileStorageConflictException(String exceptionMessage) {
        super(exceptionMessage);
    }

    public FileStorageConflictException(String message, String... args) {
        super(message, args);
    }
}
