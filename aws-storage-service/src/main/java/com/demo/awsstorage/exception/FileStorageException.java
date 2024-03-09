package com.demo.awsstorage.exception;

import com.demo.reststarter.exception.InternalErrorException;

public class FileStorageException extends InternalErrorException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable ex) {
        super(message, ex, new String[0]);
    }

    public FileStorageException(String message, String... args) {
        super(message, args);
    }

    public FileStorageException(String message, Throwable ex, String... args) {
        super(message, ex, args);
    }
}
