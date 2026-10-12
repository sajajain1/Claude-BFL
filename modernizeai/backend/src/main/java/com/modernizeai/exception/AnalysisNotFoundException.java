package com.modernizeai.exception;

import org.springframework.http.HttpStatus;

public class AnalysisNotFoundException extends ApiException {
    public AnalysisNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND",
                "No analysis with id '" + id + "' in this server session. Results are kept in memory only and expire; please re-upload.");
    }
}
