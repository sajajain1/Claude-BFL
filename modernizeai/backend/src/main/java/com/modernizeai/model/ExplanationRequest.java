package com.modernizeai.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExplanationRequest(
        @NotEmpty(message = "findingIds must contain at least one id")
        @Size(max = 25, message = "at most 25 findings can be explained per request")
        List<@Pattern(regexp = "F-\\d{1,5}", message = "finding ids look like F-001") String> findingIds) {}
