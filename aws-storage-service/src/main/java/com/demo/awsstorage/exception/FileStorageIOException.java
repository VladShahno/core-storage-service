package com.demo.awsstorage.exception;

import com.demo.reststarter.exception.InternalErrorException;

public class FileStorageIOException extends InternalErrorException {

    public FileStorageIOException(String message, Throwable cause) {
        super(message, cause, new String[0]);
    }
}
