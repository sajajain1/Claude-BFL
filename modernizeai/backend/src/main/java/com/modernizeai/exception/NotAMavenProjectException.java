package com.modernizeai.exception;

import org.springframework.http.HttpStatus;

public class NotAMavenProjectException extends ApiException {
    public NotAMavenProjectException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "NOT_A_MAVEN_PROJECT", message);
    }
}
