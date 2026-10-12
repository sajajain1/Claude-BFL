package com.modernizeai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernizeai.exception.AiResponseInvalidException;
import com.modernizeai.model.AiExplanation;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AiResponseParserTest {

    private final AiResponseParser parser = new AiResponseParser(new ObjectMapper());

    public static String entry(String id, String confidence) {
        return """
                {"findingId":"%s","whyItMayOccur":"Boot 3 uses Jakarta.","minimalFix":"Change the import.",
                 "compatibilityNotes":"Hibernate 6.","missingContext":["Transitive deps"],"confidence":"%s"}""".formatted(id, confidence);
    }

    @Test
    void parsesValidResponse() {
        List<AiExplanation> out = parser.parse("{\"explanations\":[" + entry("F-001", "high") + "," + entry("F-002", "low") + "]}",
                Set.of("F-001", "F-002"));
        assertThat(out).extracting(AiExplanation::findingId).containsExactly("F-001", "F-002");
        assertThat(out.get(0).missingContext()).containsExactly("Transitive deps");
    }

    @Test
    void toleratesMarkdownFence() {
        String raw = "```json\n{\"explanations\":[" + entry("F-001", "medium") + "]}\n```";
        assertThat(parser.parse(raw, Set.of("F-001"))).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "not json at all",
            "{\"explanations\": [",
            "[]",
            "{\"explanations\": {}}",
            "{\"explanations\": [], \"extra\": 1}",
            "{\"explanations\": [\"text\"]}",
            "{\"explanations\": [{\"findingId\":\"F-001\"}]}"
    })
    void rejectsMalformedStructures(String raw) {
        assertThatThrownBy(() -> parser.parse(raw, Set.of("F-001")))
                .isInstanceOf(AiResponseInvalidException.class)
                .hasMessageContaining("discarded");
    }

    @Test
    void rejectsUnknownFindingId() {
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + entry("F-999", "high") + "]}", Set.of("F-001")))
                .isInstanceOf(AiResponseInvalidException.class).hasMessageContaining("unknown finding id");
    }

    @Test
    void rejectsDuplicateAndMissingIds() {
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + entry("F-001", "high") + "," + entry("F-001", "high") + "]}",
                Set.of("F-001"))).hasMessageContaining("duplicate");
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + entry("F-001", "high") + "]}", Set.of("F-001", "F-002")))
                .hasMessageContaining("no explanation for F-002");
    }

    @Test
    void rejectsInvalidEnumAndExtraFields() {
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + entry("F-001", "certain") + "]}", Set.of("F-001")))
                .hasMessageContaining("confidence");
        String extra = entry("F-001", "high").replace("\"confidence\"", "\"apiKey\":\"x\",\"confidence\"");
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + extra + "]}", Set.of("F-001")))
                .hasMessageContaining("unexpected field");
    }

    @Test
    void rejectsEmptyStringsAndOversizedText() {
        String blank = entry("F-001", "high").replace("Change the import.", " ");
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + blank + "]}", Set.of("F-001"))).hasMessageContaining("minimalFix");
        String huge = entry("F-001", "high").replace("Change the import.", "x".repeat(AiResponseParser.MAX_TEXT + 1));
        assertThatThrownBy(() -> parser.parse("{\"explanations\":[" + huge + "]}", Set.of("F-001"))).hasMessageContaining("too long");
    }
}
