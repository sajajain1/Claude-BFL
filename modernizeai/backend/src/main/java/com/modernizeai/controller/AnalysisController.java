package com.modernizeai.controller;

import com.modernizeai.ai.AiExplanationService;
import com.modernizeai.exception.ApiException;
import com.modernizeai.model.AnalysisReport;
import com.modernizeai.model.ExplanationRequest;
import com.modernizeai.model.ExplanationResponse;
import com.modernizeai.service.AnalysisService;
import com.modernizeai.service.AnalysisStore;
import com.modernizeai.service.MarkdownReportRenderer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private static final String UUID_REGEX = "[0-9a-fA-F-]{36}";

    private final AnalysisService analysisService;
    private final AnalysisStore store;
    private final AiExplanationService aiService;
    private final MarkdownReportRenderer markdown;

    public AnalysisController(AnalysisService analysisService, AnalysisStore store, AiExplanationService aiService,
                              MarkdownReportRenderer markdown) {
        this.analysisService = analysisService;
        this.store = store;
        this.aiService = aiService;
        this.markdown = markdown;
    }

    /** Upload a ZIP of a Maven project and run the deterministic scan. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AnalysisReport> analyze(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "targetJavaVersion", required = false) Integer targetJavaVersion) {
        if (targetJavaVersion != null && targetJavaVersion != 17 && targetJavaVersion != 21) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "targetJavaVersion must be 17 or 21");
        }
        AnalysisReport report = analysisService.analyze(file, targetJavaVersion);
        return ResponseEntity.status(201).body(report);
    }

    @GetMapping("/{id}")
    public AnalysisReport get(@PathVariable @Pattern(regexp = UUID_REGEX, message = "invalid analysis id") String id) {
        return store.get(id);
    }

    @GetMapping(value = "/{id}/report.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnalysisReport> downloadJson(@PathVariable @Pattern(regexp = UUID_REGEX, message = "invalid analysis id") String id) {
        AnalysisReport r = store.get(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("modernizeai-report-" + id + ".json"))
                .body(r);
    }

    @GetMapping(value = "/{id}/report.md", produces = "text/markdown;charset=UTF-8")
    public ResponseEntity<byte[]> downloadMarkdown(@PathVariable @Pattern(regexp = UUID_REGEX, message = "invalid analysis id") String id) {
        AnalysisReport r = store.get(id);
        byte[] body = markdown.render(r, aiService.cached(id)).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment("modernizeai-report-" + id + ".md"))
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(body);
    }

    /** Ask Claude to explain selected findings. Returns 503 with a clear message when AI is not configured. */
    @PostMapping(value = "/{id}/explanations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ExplanationResponse explain(@PathVariable @Pattern(regexp = UUID_REGEX, message = "invalid analysis id") String id,
                                       @Valid @RequestBody ExplanationRequest request) {
        return aiService.explain(id, request.findingIds());
    }

    private static String attachment(String filename) {
        return ContentDisposition.attachment().filename(filename).build().toString();
    }
}
