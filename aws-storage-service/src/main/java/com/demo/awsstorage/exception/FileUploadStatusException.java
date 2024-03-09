package com.demo.awsstorage.exception;

import com.demo.reststarter.exception.ForbiddenException;

public class FileUploadStatusException extends ForbiddenException {

    public FileUploadStatusException(String message) {
        super(message);
    }
}
