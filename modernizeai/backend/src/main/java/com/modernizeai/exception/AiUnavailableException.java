package com.modernizeai.exception;

import org.springframework.http.HttpStatus;

/** Claude cannot be reached or is not configured. Deterministic results remain valid. */
public class AiUnavailableException extends ApiException {
    public AiUnavailableException(String code, String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }

    public AiUnavailableException(String code, String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, code, message, cause);
    }
}
