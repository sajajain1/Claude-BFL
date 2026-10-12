# Architecture

```
┌──────────────────────────┐        ┌───────────────────────────────────────────────────────────────┐
│ React + Vite (TypeScript)│  HTTP  │ Spring Boot 3.2 (Java 17), single process, no database          │
│ Home · Analyze · Results │ ─────▶ │                                                               │
│ Finding · Setup          │  /api  │ controller ─▶ service.AnalysisService                          │
└──────────────────────────┘        │                 │                                             │
                                    │                 ├─ parser.SafeZipReader   (in-memory, limits) │
                                    │                 ├─ analysis.MigrationScanner                  │
                                    │                 │    ├─ parser.PomParser   (SAX→DOM, no XXE)   │
                                    │                 │    ├─ analysis.JavaSourceParser (JavaParser) │
                                    │                 │    ├─ analysis.VersionDetector               │
                                    │                 │    └─ List<MigrationRule>  (8 rule classes)  │
                                    │                 ├─ analysis.MigrationPlanner (ordered steps)   │
                                    │                 └─ service.AnalysisStore (LRU + TTL, memory)   │
                                    │                                                               │
                                    │ ai.AiExplanationService ─▶ ai.LlmClient (interface)            │
                                    │      │                       └─ AnthropicLlmClient (SDK) ──────┼──▶ Claude API
                                    │      ├─ ExplanationPromptBuilder (snippets as JSON data)       │
                                    │      └─ AiResponseParser (strict schema validation)            │
                                    └───────────────────────────────────────────────────────────────┘
```

## Request flow

1. **Upload.** `AnalysisController` receives the multipart file. `AnalysisService` checks the name, size and ZIP magic bytes.
2. **Safe read.** `SafeZipReader` streams the archive **in memory**. It rejects `..` segments, absolute paths, drive letters, backslashes, more than 5,000 entries, more than 200 MB decompressed, or a compression ratio above 100:1. It keeps only `pom.xml`, `*.java`, `src/main/resources/*.properties|yml`, and `spring.factories`. Nothing touches the filesystem, and nothing is compiled or executed.
3. **Context.** `MigrationScanner` parses every POM (a malformed POM is recorded, not fatal) and every Java file (JavaParser AST, with a regex fallback for unparseable files). It then detects the Java and Spring Boot versions and records where each value came from.
4. **Rules.** Each `MigrationRule` is a Spring bean that returns `Finding`s. A rule that throws is logged and reported as a warning; the other rules still run.
5. **Report.** Findings are sorted (severity → certainty → category → file → line) and numbered `F-001…`. `MigrationPlanner` derives ordered steps. Only the report is stored: findings plus about 7 redacted lines of context each. The source files are dropped when the request ends.
6. **AI (optional).** For selected finding ids, `ExplanationPromptBuilder` serialises project facts and the findings as JSON inside `<analysis_data>`. The system prompt says that content is untrusted data. `AnthropicLlmClient` calls the Messages API with structured outputs (JSON schema), a timeout, one retry and typed error mapping. `AiResponseParser` then re-validates everything: no unknown fields, ids must match, enums and lengths are checked. Valid results are cached per analysis.

## Key decisions

| Decision | Why |
|---|---|
| Static analysis only; never run Maven | Running untrusted builds on a server executes arbitrary code (plugins, tests). The trade-off: parent POMs that aren't in the ZIP can't be resolved, and findings say so. |
| `VERIFIED` vs `POTENTIAL` on every finding | A pattern match is not proof. VERIFIED means the fact was observed and is documented as incompatible. POTENTIAL means it depends on context we can't see. |
| `LlmClient` interface | Tests run without network access or cost. The provider can be swapped. |
| Structured outputs **and** local validation | The schema guides the model; local validation is the security boundary. |
| In-memory store | Zero infrastructure for the MVP. Bounded (50 reports, 60 min) to cap memory. |
| Hash routing, static UI | The UI can be served by Spring Boot (`npm run build:embed`) or any static host. |
| Server-side refusal fallback opt-in | Anthropic's recommended default for this model. Disable it with `modernizeai.ai.server-side-fallback=false`. |

## Adding a rule

1. Create a `@Component` implementing `MigrationRule` in `analysis/rules`.
2. Build findings with `FindingBuilder.rule("MIG-XYZ-001")…at(path, line, content)`. This extracts a redacted snippet.
3. Choose `Certainty` honestly. Add a positive and a negative test to `RulesTest`.
4. If the rule implies a migration step, update `MigrationPlanner`. Document it in `docs/RULES.md`.
