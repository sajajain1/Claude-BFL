package com.modernizeai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernizeai.exception.AiResponseInvalidException;
import com.modernizeai.model.AiExplanation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Strictly validates Claude's JSON answer. Anything that does not match the contract is rejected
 * as a whole rather than partially shown, so users never see unvalidated AI output.
 */
@Component
public class AiResponseParser {

    static final int MAX_TEXT = 4000;
    static final int MAX_CONTEXT_ITEMS = 10;
    static final int MAX_CONTEXT_TEXT = 500;
    private static final Set<String> FIELDS = Set.of("findingId", "whyItMayOccur", "minimalFix", "compatibilityNotes",
            "missingContext", "confidence");

    private final ObjectMapper mapper;

    public AiResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<AiExplanation> parse(String raw, Set<String> expectedIds) {
        if (raw == null || raw.isBlank()) throw invalid("empty response");
        String json = stripFences(raw.trim());
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw invalid("response is not valid JSON");
        }
        if (root == null || !root.isObject()) throw invalid("top-level value must be an object");
        if (root.size() != 1 || !root.has("explanations")) throw invalid("object must contain only 'explanations'");
        JsonNode arr = root.get("explanations");
        if (!arr.isArray()) throw invalid("'explanations' must be an array");

        List<AiExplanation> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonNode n : arr) {
            if (!n.isObject()) throw invalid("each explanation must be an object");
            Iterator<String> names = n.fieldNames();
            while (names.hasNext()) {
                String f = names.next();
                if (!FIELDS.contains(f)) throw invalid("unexpected field '" + clip(f) + "'");
            }
            String id = text(n, "findingId");
            if (!expectedIds.contains(id)) throw invalid("explanation for unknown finding id '" + clip(id) + "'");
            if (!seen.add(id)) throw invalid("duplicate explanation for " + id);
            JsonNode ctx = n.get("missingContext");
            if (ctx == null || !ctx.isArray()) throw invalid("'missingContext' must be an array");
            if (ctx.size() > MAX_CONTEXT_ITEMS) throw invalid("'missingContext' has too many items");
            List<String> missing = new ArrayList<>();
            for (JsonNode c : ctx) {
                if (!c.isTextual() || c.asText().isBlank()) throw invalid("'missingContext' items must be non-empty strings");
                if (c.asText().length() > MAX_CONTEXT_TEXT) throw invalid("'missingContext' item too long");
                missing.add(c.asText().trim());
            }
            String confidence = text(n, "confidence");
            if (!ExplanationSchema.CONFIDENCE.contains(confidence)) throw invalid("'confidence' must be low, medium or high");
            out.add(new AiExplanation(id, text(n, "whyItMayOccur"), text(n, "minimalFix"), text(n, "compatibilityNotes"),
                    List.copyOf(missing), confidence));
        }
        Set<String> missingIds = new LinkedHashSet<>(expectedIds);
        missingIds.removeAll(seen);
        if (!missingIds.isEmpty()) throw invalid("no explanation for " + String.join(", ", missingIds));
        return out;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || !v.isTextual() || v.asText().isBlank()) throw invalid("'" + field + "' must be a non-empty string");
        if (v.asText().length() > MAX_TEXT) throw invalid("'" + field + "' is too long");
        return v.asText().trim();
    }

    /** Tolerates a ```json fence around the object, which some models add despite instructions. */
    static String stripFences(String s) {
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) return s.substring(firstNl + 1, lastFence).trim();
        }
        return s;
    }

    private static String clip(String s) {
        return s == null ? "null" : s.length() > 40 ? s.substring(0, 40) + "…" : s;
    }

    private static AiResponseInvalidException invalid(String why) {
        return new AiResponseInvalidException("Claude's response failed validation (" + why + "), so it was discarded. "
                + "The deterministic findings are unaffected; you can retry.");
    }
}
