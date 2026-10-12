# ModernizeAI

**AI-assisted migration analysis for legacy Java and Spring Boot projects.**

Upload a ZIP of a Maven project. ModernizeAI statically scans it for the issues that commonly block an upgrade from
Java 8/11 and Spring Boot 2.x to Java 17+ and Spring Boot 3.x, and shows each one with its file, line and evidence.
Optionally, it asks Claude to explain selected findings and propose a minimal fix.

> ⚠️ Recommendations require review. ModernizeAI never builds, runs or rewrites your code. Findings marked
> **POTENTIAL** are pattern matches that need human judgement, and even **VERIFIED** findings describe observed facts,
> not proof that a migration will fail.

| | |
|---|---|
| Backend | Java 17 · Spring Boot 3.2.12 · Maven · JavaParser 3.26 · Anthropic Java SDK 2.34 |
| Frontend | React 18 · Vite 5 · TypeScript 5 |
| Storage | In memory only (no database) |
| Docs | [API](docs/API.md) · [Architecture](docs/ARCHITECTURE.md) · [Rule catalog](docs/RULES.md) · [Demo script & launch guide](docs/DEMO.md) |

---

## Features

- **Safe project upload.** ZIP up to 20 MB, read entirely in memory. Rejects path traversal, absolute paths, more than
  5,000 entries, and zip bombs (200 MB / 100:1 ratio limits). Never executes uploaded code or Maven.
- **POM analysis.** Detects the Java and Spring Boot versions and records where each came from (`pom.xml:8`).
  Handles parents, BOM imports, properties, compiler plugin config and multi-module projects. XXE-safe parser.
  Malformed POMs become a finding instead of failing the analysis.
- **27 deterministic rules** across javax→jakarta, Spring Boot version, Java version, removed Spring Security/MVC
  APIs, renamed properties, `spring.factories`, dependency conflicts, missing or unresolvable versions, incompatible
  libraries (Springfox, Sleuth, …), and JDK internals. See [docs/RULES.md](docs/RULES.md).
- **Every finding has** a rule id, severity, VERIFIED/POTENTIAL certainty, file and line, evidence, a redacted code
  snippet, an explanation, a suggested next step and documentation links.
- **Dashboard.** Project summary, severity distribution, filters, findings grouped by category, a finding detail view,
  ordered migration steps, and JSON and Markdown export.
- **Claude explanations (optional).** Only the selected findings (max 5 per request) and their redacted snippets
  are sent. Uploaded code is passed as JSON data, and the system prompt tells Claude not to follow instructions inside
  it. Responses use structured outputs and are re-validated against a strict schema; invalid output is discarded. With
  no API key, the UI says so plainly and everything else keeps working.

## Quick start (local)

### Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 17 or newer (21 works) | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 18+ (tested with 22) | `node -v` |

All of these are free. No paid services are needed to run the scanner.

### 1. Start the backend

```bash
cd modernizeai/backend
mvn spring-boot:run            # http://localhost:8080  (first run downloads dependencies)
```

Check it is up: `curl http://localhost:8080/actuator/health` should return `{"status":"UP",...}`.

### 2. Start the frontend (second terminal)

```bash
cd modernizeai/frontend
npm install
npm run dev                    # http://localhost:5173  (proxies /api to :8080)
```

Open <http://localhost:5173>, go to **Analyze**, and upload `modernizeai/samples/legacy-shop.zip`.

### Alternative: one process, one port

```bash
cd modernizeai/frontend && npm install && npm run build:embed   # copies the UI into backend/src/main/resources/static
cd ../backend && mvn -DskipTests package
java -jar target/modernizeai-backend-0.1.0.jar                 # UI + API on http://localhost:8080
```

### Try it from the command line

```bash
curl -F file=@modernizeai/samples/legacy-shop.zip http://localhost:8080/api/analyses | jq '.countsBySeverity'
```

## Enabling Claude (optional)

A Claude.ai chat subscription **does not** include API access. The Anthropic API is billed separately, per token.

1. Create a key at <https://console.anthropic.com> and add credit.
2. Export it in the shell that runs the backend and restart:
   ```bash
   export ANTHROPIC_API_KEY="sk-ant-..."        # PowerShell: $env:ANTHROPIC_API_KEY="sk-ant-..."
   export ANTHROPIC_MODEL="claude-opus-5-5"     # optional; e.g. claude-haiku-5-5 for lower cost
   cd modernizeai/backend && mvn spring-boot:run
   ```
3. The **Claude setup** page should show “Enabled”. Open a finding and click **Explain with Claude**.

The key is read only from the environment. It is never logged, returned by the API, or sent to the browser. See
`.env.example` for every variable.

Notes on the Claude request: it uses the official Anthropic Java SDK with structured JSON output, `effort: medium`,
a 90 s timeout, one retry, and max 8,000 output tokens. Anthropic's server-side refusal fallback is opted into
(`fallbacks: "default"`) as recommended for this model. Set `modernizeai.ai.server-side-fallback=false` to disable it.

## Configuration

`backend/src/main/resources/application.yml` (override any value with an environment variable, e.g.
`MODERNIZEAI_UPLOAD_MAX_ZIP_BYTES=10485760`):

| Property | Default | Meaning |
|---|---|---|
| `modernizeai.upload.max-zip-bytes` | 20 MB | Max upload size (also raise `spring.servlet.multipart.max-file-size`) |
| `modernizeai.upload.max-entries` | 5000 | Max ZIP entries |
| `modernizeai.upload.max-total-uncompressed-bytes` | 200 MB | Zip-bomb guard |
| `modernizeai.upload.max-file-bytes` | 2 MB | Larger source files are skipped with a warning |
| `modernizeai.upload.max-compression-ratio` | 100 | Zip-bomb guard |
| `modernizeai.analysis.default-target-java-version` | 17 | Used when the request omits it |
| `modernizeai.analysis.max-stored-analyses` / `result-ttl-minutes` | 50 / 60 | In-memory retention |
| `modernizeai.ai.model` | `claude-opus-5-5` | Overridden by `ANTHROPIC_MODEL` |
| `modernizeai.ai.timeout-seconds` | 90 | Claude HTTP timeout |
| `modernizeai.ai.max-findings-per-request` | 5 | Cap on findings per Claude call |
| `modernizeai.ai.max-snippet-chars` | 1500 | Cap on source characters sent per finding |
| `modernizeai.ai.server-side-fallback` | true | Anthropic refusal fallback opt-in |
| `PORT` | 8080 | HTTP port |
| `MODERNIZEAI_CORS_ORIGINS` | localhost:5173 | Browser origins allowed to call `/api` |

## Tests

```bash
cd modernizeai/backend && mvn test        # 113 tests: parsers, rules, ZIP security, AI parsing, REST
cd modernizeai/frontend && npm test       # 4 tests: filtering/grouping helpers, router
cd modernizeai/frontend && npm run typecheck
```

The automated tests never call the real Claude API: `LlmClient` is mocked, so they need no key and cost nothing.

| Area | Test class |
|---|---|
| POM parsing, malformed XML, XXE | `PomParserTest` |
| Java / Spring Boot version detection | `VersionDetectorTest` |
| Every rule (positive and negative cases) | `RulesTest` |
| ZIP path traversal, zip bombs, invalid/empty/corrupt uploads | `SafeZipReaderTest` |
| Secret redaction in snippets | `SecretRedactorTest` |
| AI JSON validation, invalid JSON | `AiResponseParserTest` |
| AI service: no key, caching, redaction, prompt-injection containment | `AiExplanationServiceTest` |
| Claude request shape (schema, fallback, key not in body) | `AnthropicLlmClientTest` |
| REST endpoints and validation (MockMvc) | `AnalysisControllerTest` |
| Full scans of both sample projects | `SampleProjectScanTest` |

## Sample projects

- `samples/legacy-shop/` (+ `.zip`): Spring Boot 2.5 / Java 8 with roughly 30 deliberate issues, a fake password
  (to show redaction) and a prompt-injection comment (to show untrusted-input handling).
- `samples/modern-service/` (+ `.zip`): a clean Boot 3.2 / Java 17 project. It should produce **zero** findings,
  which is a false-positive check.
- Rebuild the zips with `samples/make-sample-zips.sh`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| UI says “Cannot reach the ModernizeAI backend” | Start the backend on port 8080, or set `VITE_API_BASE` |
| `Port 8080 already in use` | `PORT=8090 mvn spring-boot:run`, and change the target in `frontend/vite.config.ts` |
| `UNSAFE_ARCHIVE` | The ZIP has `../` or absolute paths, too many files, or an extreme compression ratio. Re-zip the project folder normally |
| `NOT_A_MAVEN_PROJECT` | The ZIP has no `pom.xml`. Gradle is not supported yet |
| `UPLOAD_TOO_LARGE` | Exclude `target/`, `.git/`, `node_modules/` before zipping, or raise the limits |
| Java/Boot version shows “not detected” | It's inherited from a parent POM that isn't in the ZIP. Check `mvn help:effective-pom` locally |
| `AI_NOT_CONFIGURED` | Export `ANTHROPIC_API_KEY` in the same shell and restart the backend |
| `AI_AUTH_FAILED` / `AI_REQUEST_REJECTED` | Invalid key, or no API credit (the message includes Anthropic's reason) |
| `AI_RESPONSE_INVALID` | Claude's JSON failed validation and was discarded; retry with fewer findings |
| `ANALYSIS_NOT_FOUND` | Reports live in memory for 60 min and are lost on restart. Upload again |
| Maven cannot download dependencies behind a proxy | Configure `~/.m2/settings.xml` proxies |

## Security model

- Never executes uploaded code, Maven, or build plugins. Only text parsing is done.
- Nothing is extracted to disk. Archives are read in memory under strict limits, and unsafe entry names reject the whole upload.
- XML parsing has DOCTYPE and external entities disabled.
- Only redacted snippets of about 7 lines are kept, in memory, with a TTL. Values that look like passwords, tokens, AWS keys,
  `sk-ant-…` keys, private keys and JDBC credentials are masked before storage, display, export or sending to Claude.
- Uploaded content is untrusted: it reaches Claude only as JSON string values, with explicit instructions to ignore
  embedded instructions, and the output is schema-validated.
- API key only from `ANTHROPIC_API_KEY`. Anthropic SDK and HTTP-client logging is capped at WARN. Error responses never include
  stack traces or internal messages. Only `health` and `info` actuator endpoints are exposed.
- No authentication in the MVP: run it locally or behind your own access control.

## Known limitations

- **Maven only.** Gradle builds are not parsed.
- **Static and single-file.** Parent POMs, BOMs and transitive dependencies outside the ZIP can't be resolved, so
  some dependency checks are POTENTIAL. No type resolution: method-name rules (e.g. `antMatchers`) are POTENTIAL.
- The rule set is curated, not exhaustive. Behavioural changes (e.g. trailing-slash matching, Hibernate 6 query
  semantics) are not detected and are only mentioned in the plan.
- In-memory storage: results vanish on restart, and the store is per-instance.
- No automatic code rewriting (by design for the MVP).
- No user accounts or rate limiting. Don't expose it publicly as is.
- Claude explanations depend on short snippets and can be wrong; they are labelled and need review.

## Roadmap

1. Gradle support and `mvn help:effective-pom` import (user-supplied) for exact dependency resolution.
2. More rules: Hibernate 6 / JPA query changes, trailing-slash matching, `RestTemplate` → `RestClient`, Jackson 2.15+.
3. Patch suggestions as reviewable diffs, generated with OpenRewrite recipes rather than free-form AI edits.
4. GitHub integration: scan a repo or PR, and post a migration report as a check run.
5. Persistent projects, team sharing, auth (OIDC), rate limiting, and usage-based AI budgeting.
6. Evaluation set: real open-source migrations to measure rule precision and recall, and AI explanation quality.

## Free-first deployment options

Running locally costs nothing. If you want a hosted demo, any container host with a free tier that runs a Java 17
JAR will work (build with `npm run build:embed` + `mvn package` and run the JAR with `PORT` set). Free tiers change
often, so check current terms. Keep the API key in the host's secret store, and add authentication before sharing a public URL.

## Project structure

```
modernizeai/
├── backend/                     Spring Boot service
│   ├── pom.xml
│   └── src/main/java/com/modernizeai/
│       ├── controller/          AnalysisController, StatusController
│       ├── service/             AnalysisService, AnalysisStore, MarkdownReportRenderer
│       ├── parser/              SafeZipReader, ProjectFiles, PomParser, PomModel
│       ├── analysis/            MigrationScanner, VersionDetector, JavaSourceParser, MigrationPlanner,
│       │   │                    FindingBuilder, Snippets, SecretRedactor, VersionUtils, References
│       │   └── rules/           8 rule classes (27 rule ids)
│       ├── ai/                  LlmClient, AnthropicLlmClient, AiExplanationService,
│       │                        ExplanationPromptBuilder, ExplanationSchema, AiResponseParser
│       ├── model/               Finding, AnalysisReport, ProjectSummary, AiExplanation, ...
│       ├── exception/           ApiException hierarchy, GlobalExceptionHandler
│       └── config/              ModernizeAiProperties, WebConfig
├── frontend/                    React + Vite + TypeScript
│   └── src/{pages,components}/  Home, Analyze, Results, FindingDetail, Setup
├── samples/                     legacy-shop, modern-service (+ zips)
├── docs/                        API.md, ARCHITECTURE.md, RULES.md, DEMO.md
└── .env.example
```

## License

No license has been chosen yet. Add one (e.g. Apache-2.0 or MIT) before inviting outside contributors.
