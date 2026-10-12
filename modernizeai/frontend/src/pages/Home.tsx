import { href } from "../router";

export function Home() {
  return (
    <div className="page">
      <section className="hero">
        <h1>Find what breaks before you upgrade Java and Spring Boot.</h1>
        <p className="lead">
          ModernizeAI scans a Maven project for the migration issues that commonly block a move from Java 8/11 and
          Spring Boot 2.x to Java 17+ and Spring Boot 3.x, shows the exact file and line, and can ask Claude to
          explain a finding and propose a minimal fix.
        </p>
        <div className="actions">
          <a className="button primary" href={href.analyze}>
            Analyze a project
          </a>
          <a className="button" href={href.setup}>
            Set up Claude (optional)
          </a>
        </div>
      </section>

      <section className="grid3">
        <div className="card">
          <h3>Deterministic rules</h3>
          <p>
            javax → jakarta imports, Spring Boot 2.x parents, Java versions below target, removed Spring Security /
            Spring MVC APIs, renamed properties, and dependency conflicts the POM proves.
          </p>
        </div>
        <div className="card">
          <h3>Evidence, not guesses</h3>
          <p>
            Every finding has a rule id, severity, file and line, the evidence it matched, and whether it is{" "}
            <em>verified</em> or only a <em>potential</em> issue that needs review.
          </p>
        </div>
        <div className="card">
          <h3>Claude, when you want it</h3>
          <p>
            Selected findings and short, redacted snippets are sent to Claude. Its answers are schema-validated, and
            nothing AI-generated is shown without being labelled.
          </p>
        </div>
      </section>

      <section className="card">
        <h3>How your code is handled</h3>
        <ul className="checklist">
          <li>Your ZIP is read in memory and is never written to disk, built or executed.</li>
          <li>Archives with path traversal, too many files or zip-bomb ratios are rejected.</li>
          <li>Only findings and a few redacted lines of context are kept, in memory, for up to an hour.</li>
          <li>Nothing is sent to Claude unless you click “Explain”, and only the findings you choose.</li>
          <li>No code is rewritten automatically. You review and apply changes yourself.</li>
        </ul>
      </section>
    </div>
  );
}
