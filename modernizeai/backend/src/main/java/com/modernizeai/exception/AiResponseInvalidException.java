package com.modernizeai.exception;

import org.springframework.http.HttpStatus;

/** Claude answered but the answer failed schema validation, so it is discarded rather than shown. */
public class AiResponseInvalidException extends ApiException {
    public AiResponseInvalidException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_RESPONSE_INVALID", message);
    }
}
