package com.modernizeai.exception;

import org.springframework.http.HttpStatus;

/** The archive was rejected for safety reasons (path traversal, zip bomb, too many entries). */
public class UnsafeArchiveException extends ApiException {
    public UnsafeArchiveException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "UNSAFE_ARCHIVE", message);
    }
}
