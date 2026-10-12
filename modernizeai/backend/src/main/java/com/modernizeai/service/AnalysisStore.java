package com.modernizeai.service;

import com.modernizeai.config.ModernizeAiProperties;
import com.modernizeai.exception.AnalysisNotFoundException;
import com.modernizeai.model.AiExplanation;
import com.modernizeai.model.AnalysisReport;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bounded, time-limited in-memory store for reports of the current server session. Only reports
 * (findings + redacted snippets) are stored, never the uploaded archive or full source files.
 */
@Component
public class AnalysisStore {

    record StoredAnalysis(AnalysisReport report, Map<String, AiExplanation> explanations, Instant storedAt) {}

    private final int maxEntries;
    private final Duration ttl;
    private final Map<String, StoredAnalysis> entries;

    public AnalysisStore(ModernizeAiProperties properties) {
        this.maxEntries = properties.analysis().maxStoredAnalyses();
        this.ttl = Duration.ofMinutes(properties.analysis().resultTtlMinutes());
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, StoredAnalysis> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public synchronized void put(AnalysisReport report) {
        entries.put(report.analysisId(), new StoredAnalysis(report, new ConcurrentHashMap<>(), Instant.now()));
    }

    public synchronized AnalysisReport get(String id) {
        return entry(id).report();
    }

    public synchronized Map<String, AiExplanation> explanations(String id) {
        return entry(id).explanations();
    }

    public synchronized Optional<AnalysisReport> find(String id) {
        try {
            return Optional.of(get(id));
        } catch (AnalysisNotFoundException e) {
            return Optional.empty();
        }
    }

    private StoredAnalysis entry(String id) {
        StoredAnalysis e = entries.get(id);
        if (e == null || e.storedAt().plus(ttl).isBefore(Instant.now())) {
            if (e != null) entries.remove(id);
            throw new AnalysisNotFoundException(id);
        }
        return e;
    }
}
