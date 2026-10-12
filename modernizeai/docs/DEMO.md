# Five-minute demo script

**Setup before recording:** backend and frontend running (see README), browser at <http://localhost:5173>, a terminal
open in `modernizeai/`, and `samples/legacy-shop/src/main/java/com/example/shop/config/SecurityConfig.java` open in
an editor. Decide up front whether you're showing Claude live (API key set) or not. Never fake that part.

| Time | Show | Say |
|---|---|---|
| 0:00–0:30 | Home page | “Upgrading Java 8 / Spring Boot 2 apps to Java 17 / Boot 3 is a known slog: javax→jakarta, removed Security APIs, renamed properties, dependency drift. ModernizeAI finds those spots before you start, with evidence, and doesn't touch your code.” |
| 0:30–1:00 | Analyze page → upload `samples/legacy-shop.zip` | “It's a ZIP of a Maven project. It's read in memory, never built or executed. Archives with path traversal or zip-bomb ratios are rejected.” (Optional: upload a renamed `.txt` to show the validation error.) |
| 1:00–2:15 | Results dashboard | Point out the detected Java 8 and Boot 2.5.14 *with their source lines*, the severity bar, and the category groups. Then: “Every finding says whether it's **verified**, a fact we observed, or **potential**, a pattern that needs a human. We don't claim a regex proves a failure.” Filter to HIGH, search `Security`. |
| 2:15–3:00 | Open F-006 (WebSecurityConfigurerAdapter) | Show the file:line, the snippet, why it matters, the next step and the official link. Then open a `application.properties` finding: “The demo password is masked before anything is stored or sent anywhere.” |
| 3:00–4:00 | Claude | **With a key:** click *Explain with Claude*. “Only this finding and those ~7 redacted lines go to Claude. The answer has to match a strict JSON schema, or we throw it away.” Point at *missing context* and *confidence*. Mention the prompt-injection comment in SecurityConfig.java: “That's treated as data; Claude is told never to follow it.” **Without a key:** click it anyway. “No key, so it says so. It doesn't invent an AI answer, and the scanner keeps working.” Then show the Setup page. |
| 4:00–4:30 | Recommended steps + Download Markdown | “An ordered plan: Boot 2.7 first, Java 17, Security config, then Boot 3 with jakarta. It exports to Markdown or JSON for a ticket or PR.” |
| 4:30–5:00 | Terminal: `mvn test` result, then upload `samples/modern-service.zip` | “113 backend tests, including ZIP attacks and mocked AI responses. And a clean Boot 3 project produces zero findings, our false-positive check.” Close on the roadmap: Gradle, OpenRewrite-backed patches, a GitHub PR check. |

### Recording tips

- Use OBS Studio (free) or your OS's built-in recorder at 1080p. Zoom the browser to 125%.
- Pre-run one analysis so dependencies are warm, but do the main upload live.
- If Claude is live, run one explanation beforehand to confirm the key and credit work; say on camera which model answered.

---

# Publishing and an honest Claude for Startups application

## Publish the repository

1. Remove anything private: `git status`, and check that no `.env` or keys are tracked (`git grep -n "sk-ant-"` should show only placeholders and tests).
2. Choose a license (Apache-2.0 is common for dev tools) and add `LICENSE`.
3. Push to GitHub. Add a short description, topics (`java`, `spring-boot`, `migration`, `static-analysis`, `claude`),
   and a screenshot of the dashboard in the README.
4. Turn on GitHub's secret scanning and Dependabot (free for public repos). The included workflow
   `.github/workflows/modernizeai-ci.yml` runs the backend and frontend tests on every push.
5. Tag `v0.1.0` and write release notes that list what works and the known limitations.

## Prepare the application

Check the current program page for eligibility and the actual questions; programs change. Whatever the form asks,
keep every claim verifiable:

- **Problem:** Java 8/11 and Spring Boot 2.x estates that must move to Java 17+ / Boot 3 (Boot 2.x OSS support has
  ended). The work is large, repetitive and risky.
- **What exists today (link the repo and the demo video):** a working MVP with 27 deterministic rules, safe upload
  handling, a dashboard, JSON/Markdown export, and Claude explanations with schema validation and prompt-injection
  safeguards. 113 automated backend tests.
- **Why Claude:** explaining migration impact in context, proposing minimal fixes, and stating what's unknown. The
  deterministic engine keeps it grounded; Claude adds judgement on top.
- **What credits would fund:** evaluating explanation quality on real open-source migrations, building
  OpenRewrite-backed patch generation, and a hosted beta for design partners.
- **Traction:** report only real numbers (users, design-partner conversations, GitHub stars). If it's zero, say it's
  pre-launch and give your plan for the first 10 teams.
- **Don't** claim customers, accuracy percentages, or AI features you haven't run. If you haven't tested Claude with a
  real key yet, say the integration is implemented and unit-tested with mocked responses, pending live validation.
