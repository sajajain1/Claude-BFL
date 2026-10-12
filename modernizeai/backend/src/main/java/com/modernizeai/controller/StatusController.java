package com.modernizeai.controller;

import com.modernizeai.ai.AiExplanationService;
import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.model.AiStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight status for the UI. Liveness/readiness probes use /actuator/health. */
@RestController
@RequestMapping("/api")
public class StatusController {

    private final AiExplanationService aiService;
    private final ModernizeAiProperties properties;

    public StatusController(AiExplanationService aiService, ModernizeAiProperties properties) {
        this.aiService = aiService;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("status", "UP");
        b.put("service", "modernizeai-backend");
        return b;
    }

    @GetMapping("/ai/status")
    public AiStatus aiStatus() {
        return aiService.status();
    }

    @GetMapping("/limits")
    public Map<String, Object> limits() {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("maxZipMegabytes", properties.upload().maxZipBytes() / (1024 * 1024));
        b.put("maxEntries", properties.upload().maxEntries());
        b.put("supportedTargetJavaVersions", new int[] {17, 21});
        b.put("defaultTargetJavaVersion", properties.analysis().defaultTargetJavaVersion());
        b.put("maxFindingsPerAiRequest", properties.ai().maxFindingsPerRequest());
        return b;
    }
}
