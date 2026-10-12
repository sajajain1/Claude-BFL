package com.modernizeai.exception;

import org.springframework.http.HttpStatus;

public class InvalidUploadException extends ApiException {
    public InvalidUploadException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_UPLOAD", message);
    }
}
