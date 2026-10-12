import { useEffect, useState } from "react";
import { api, CATEGORY_LABELS, type AnalysisReport } from "../api";
import { AiExplanationCard } from "../components/AiExplanationCard";
import { CertaintyBadge, SeverityBadge } from "../components/Badges";
import { Disclaimer, ErrorBanner, Spinner } from "../components/Feedback";
import { saveExplanations, useExplanation } from "../explanations";
import { location } from "../report";
import { href } from "../router";

export function FindingDetail({ id, findingId }: { id: string; findingId: string }) {
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [aiBusy, setAiBusy] = useState(false);
  const [aiError, setAiError] = useState<unknown>(null);
  const [model, setModel] = useState<string | undefined>();
  const ai = useExplanation(id, findingId);

  useEffect(() => {
    api.get(id).then(setReport, setError);
  }, [id]);

  if (error) return <div className="page"><ErrorBanner error={error} /></div>;
  if (!report) return <div className="page"><Spinner label="Loading finding…" /></div>;

  const idx = report.findings.findIndex((f) => f.id === findingId);
  const f = report.findings[idx];
  if (!f) {
    return (
      <div className="page">
        <ErrorBanner error={new Error(`Finding ${findingId} does not exist in this report.`)} />
        <a href={href.results(id)}>← Back to report</a>
      </div>
    );
  }
  const prev = report.findings[idx - 1];
  const next = report.findings[idx + 1];

  async function explain() {
    setAiBusy(true);
    setAiError(null);
    try {
      const res = await api.explain(id, [findingId]);
      saveExplanations(id, res.explanations);
      setModel(res.model);
    } catch (e) {
      setAiError(e);
    } finally {
      setAiBusy(false);
    }
  }

  return (
    <div className="page narrow-wide">
      <nav className="crumbs">
        <a href={href.results(id)}>← Report</a>
        <span className="spacer" />
        {prev && <a href={href.finding(id, prev.id)}>‹ {prev.id}</a>}
        {next && <a href={href.finding(id, next.id)}>{next.id} ›</a>}
      </nav>

      <header className="finding-header">
        <span className="muted">
          {f.id} · {CATEGORY_LABELS[f.category]}
        </span>
        <h1>{f.title}</h1>
        <div className="finding-meta">
          <SeverityBadge severity={f.severity} />
          <CertaintyBadge certainty={f.certainty} />
          <code>{f.ruleId}</code>
        </div>
      </header>

      {f.certainty === "POTENTIAL" && (
        <div className="banner note">
          <strong>Potential issue.</strong> The pattern matched, but whether it is a real problem depends on context the
          scanner cannot see. Treat it as something to check, not a confirmed defect.
        </div>
      )}

      <section className="card">
        <h3>Location</h3>
        <p className="mono">{location(f)}</p>
        <h3>Evidence</h3>
        <p>{f.evidence}</p>
        {f.snippet && (
          <>
            <h3>Code context</h3>
            <pre className="snippet">
              <code>{f.snippet}</code>
            </pre>
            <p className="muted small">Values that look like secrets are masked.</p>
          </>
        )}
      </section>

      <section className="card">
        <h3>Why it matters</h3>
        <p>{f.explanation}</p>
        <h3>Suggested next step</h3>
        <p>{f.suggestedNextStep}</p>
        {f.references.length > 0 && (
          <>
            <h3>References</h3>
            <ul>
              {f.references.map((r) => (
                <li key={r}>
                  <a href={r} target="_blank" rel="noreferrer noopener">
                    {r}
                  </a>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>

      <section className="card">
        <div className="title-row">
          <h3>Claude explanation</h3>
          {!ai && (
            <button className="primary" onClick={explain} disabled={aiBusy}>
              {aiBusy ? "Asking Claude…" : "Explain with Claude"}
            </button>
          )}
        </div>
        {!ai && !aiBusy && aiError == null && (
          <p className="muted small">
            Sends this finding, its evidence and the redacted snippet above (nothing else) to Claude. Requires
            ANTHROPIC_API_KEY on the server.
          </p>
        )}
        {aiBusy && <Spinner label="Waiting for Claude…" />}
        {aiError != null && <ErrorBanner error={aiError} />}
        {ai && <AiExplanationCard e={ai} model={model} />}
      </section>

      <Disclaimer />
    </div>
  );
}
