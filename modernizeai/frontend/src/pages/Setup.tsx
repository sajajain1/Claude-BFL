import { useEffect, useState } from "react";
import { api, type AiStatus } from "../api";
import { ErrorBanner, Spinner } from "../components/Feedback";

export function Setup() {
  const [status, setStatus] = useState<AiStatus | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    api.aiStatus().then(setStatus, setError);
  }, []);

  return (
    <div className="page narrow">
      <h1>Claude setup</h1>
      <p className="lead">
        The scanner works fully without AI. Claude explanations are optional and need an Anthropic API key with
        credit. A Claude.ai chat subscription does <strong>not</strong> include API access; the API is billed
        separately.
      </p>

      <section className="card">
        <h3>Current status</h3>
        {error != null ? (
          <ErrorBanner error={error} />
        ) : !status ? (
          <Spinner label="Checking backend…" />
        ) : (
          <div className={`banner ${status.configured ? "ok" : "note"}`}>
            <strong>{status.configured ? "Enabled" : "Not configured"}</strong> · model <code>{status.model}</code>
            <p>{status.message}</p>
          </div>
        )}
        <p className="muted small">
          “Enabled” only means a key is present. The key is first used when you request an explanation; invalid keys,
          exhausted credit or network problems are reported then.
        </p>
      </section>

      <section className="card">
        <h3>Enable Claude explanations</h3>
        <ol className="steps">
          <li>
            Create an API key in the Anthropic Console (<code>console.anthropic.com</code>) and add credit to the account.
          </li>
          <li>
            Export it in the shell that starts the backend. Never commit it or put it in frontend code:
            <pre className="snippet">
              <code>{`export ANTHROPIC_API_KEY="sk-ant-..."     # macOS / Linux
# PowerShell: $env:ANTHROPIC_API_KEY="sk-ant-..."
cd backend && mvn spring-boot:run`}</code>
            </pre>
          </li>
          <li>
            Optional: choose another model with <code>ANTHROPIC_MODEL</code> (for example <code>claude-haiku-5-5</code> for
            lower cost). The default is <code>claude-opus-5-5</code>.
          </li>
          <li>Reload this page; the status above should say “Enabled”.</li>
        </ol>
      </section>

      <section className="card">
        <h3>What is sent to Claude</h3>
        <ul className="checklist">
          <li>Only the findings you select (at most 5 per request).</li>
          <li>For each: rule id, title, evidence, file path, line, and about 7 lines of redacted code context.</li>
          <li>Detected and target Java / Spring Boot versions.</li>
          <li>Never the whole project, the archive, or values that look like passwords, tokens or keys.</li>
          <li>
            Uploaded code is wrapped as data, and Claude is instructed not to follow instructions inside it. Responses
            must match a JSON schema or they are discarded.
          </li>
        </ul>
      </section>
    </div>
  );
}
