package com.modernizeai.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernizeai.ai.AiResponseParserTest;
import com.modernizeai.ai.LlmClient;
import com.modernizeai.support.TestSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean LlmClient llm;

    @BeforeEach
    void noAiByDefault() {
        when(llm.isConfigured()).thenReturn(false);
        when(llm.modelName()).thenReturn("claude-opus-5-5");
    }

    private MockMultipartFile sampleZip() {
        return new MockMultipartFile("file", "legacy-shop.zip", "application/zip",
                TestSupport.zipDirectory(TestSupport.SAMPLES.resolve("legacy-shop")));
    }

    private JsonNode upload() throws Exception {
        String body = mvc.perform(multipart("/api/analyses").file(sampleZip()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void analyzesSampleProject() throws Exception {
        mvc.perform(multipart("/api/analyses").file(sampleZip()).param("targetJavaVersion", "21"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.analysisId").isNotEmpty())
                .andExpect(jsonPath("$.targetJavaVersion").value(21))
                .andExpect(jsonPath("$.project.springBootVersion").value("2.5.14"))
                .andExpect(jsonPath("$.project.javaVersion").value("8"))
                .andExpect(jsonPath("$.countsBySeverity.HIGH").value(greaterThan(0)))
                .andExpect(jsonPath("$.findings[*].ruleId", hasItem("MIG-JAKARTA-001")))
                .andExpect(jsonPath("$.findings[0].certainty").exists())
                .andExpect(jsonPath("$.disclaimer").value(containsString("review")));
    }

    @Test
    void retrievesAndExportsReport() throws Exception {
        String id = upload().get("analysisId").asText();
        mvc.perform(get("/api/analyses/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.analysisId").value(id));
        mvc.perform(get("/api/analyses/" + id + "/report.json"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(jsonPath("$.findings").isArray());
        mvc.perform(get("/api/analyses/" + id + "/report.md"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/markdown"))
                .andExpect(content().string(containsString("# ModernizeAI migration report")))
                .andExpect(content().string(containsString("MIG-BOOT-001")));
    }

    @Test
    void rejectsNonZipUpload() throws Exception {
        mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
        mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file", "fake.zip", "application/zip", "hello".getBytes())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("not a ZIP")));
    }

    @Test
    void rejectsMissingAndEmptyFile() throws Exception {
        mvc.perform(multipart("/api/analyses")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_UPLOAD"));
        mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file", "empty.zip", "application/zip", new byte[0])))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPathTraversalArchive() throws Exception {
        byte[] zip = TestSupport.zip(Map.of("pom.xml", "<project/>", "../../etc/cron.d/evil.java", "x"));
        mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file", "evil.zip", "application/zip", zip)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNSAFE_ARCHIVE"));
    }

    @Test
    void rejectsArchiveWithoutPom() throws Exception {
        byte[] zip = TestSupport.zip(Map.of("src/A.java", "class A {}"));
        mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file", "x.zip", "application/zip", zip)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_A_MAVEN_PROJECT"));
    }

    @Test
    void malformedPomStillProducesReport() throws Exception {
        byte[] zip = TestSupport.zip(Map.of("pom.xml", "<project><dependencies></project>",
                "src/main/java/A.java", "import javax.servlet.Filter;\nclass A {}"));
        mvc.perform(multipart("/api/analyses").file(new MockMultipartFile("file", "x.zip", "application/zip", zip)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.findings[*].ruleId", hasItem("MIG-POM-001")))
                .andExpect(jsonPath("$.findings[*].ruleId", hasItem("MIG-JAKARTA-001")));
    }

    @Test
    void validatesTargetJavaVersion() throws Exception {
        mvc.perform(multipart("/api/analyses").file(sampleZip()).param("targetJavaVersion", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(multipart("/api/analyses").file(sampleZip()).param("targetJavaVersion", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAndInvalidIds() throws Exception {
        mvc.perform(get("/api/analyses/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANALYSIS_NOT_FOUND"));
        mvc.perform(get("/api/analyses/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void explanationsReturn503WhenAiNotConfigured() throws Exception {
        String id = upload().get("analysisId").asText();
        mvc.perform(post("/api/analyses/" + id + "/explanations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingIds\":[\"F-001\"]}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.message").value(containsString("ANTHROPIC_API_KEY")));
    }

    @Test
    void explanationRequestValidation() throws Exception {
        String id = upload().get("analysisId").asText();
        String url = "/api/analyses/" + id + "/explanations";
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"findingIds\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"findingIds\":[\"DROP TABLE\"]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("F-001")));
        mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void explanationsWithMockedClaude() throws Exception {
        String id = upload().get("analysisId").asText();
        when(llm.isConfigured()).thenReturn(true);
        when(llm.complete(anyString(), anyString(), any())).thenReturn(new LlmClient.LlmResult(
                "{\"explanations\":[" + AiResponseParserTest.entry("F-001", "high") + "]}", "claude-opus-5-5", "end_turn"));
        mvc.perform(post("/api/analyses/" + id + "/explanations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"findingIds\":[\"F-001\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.explanations[0].findingId").value("F-001"))
                .andExpect(jsonPath("$.explanations[0].confidence").value("high"))
                .andExpect(jsonPath("$.notice").value(containsString("verify")));
        mvc.perform(get("/api/analyses/" + id + "/report.md"))
                .andExpect(content().string(containsString("AI explanation (Claude")));
    }

    @Test
    void statusAndHealthEndpoints() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/api/ai/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.message").value(containsString("ANTHROPIC_API_KEY")));
        mvc.perform(get("/api/limits")).andExpect(status().isOk()).andExpect(jsonPath("$.maxZipMegabytes").value(20));
        mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
    }
}
