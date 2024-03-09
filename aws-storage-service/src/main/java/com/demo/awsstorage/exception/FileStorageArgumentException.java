package com.demo.awsstorage.exception;

import com.demo.reststarter.exception.BadRequestRestException;

public class FileStorageArgumentException extends BadRequestRestException {

    public FileStorageArgumentException(String message) {
        super(message);
    }
}
