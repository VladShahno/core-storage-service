package com.demo.awsstorage.exception;

import com.demo.reststarter.exception.NotFoundException;

public class FileStorageNotFoundException extends NotFoundException {

    public FileStorageNotFoundException(String message) {
        super(message);
    }
}
