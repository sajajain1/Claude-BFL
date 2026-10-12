import { useEffect, useMemo, useState } from "react";
import { api, CATEGORY_LABELS, SEVERITIES, type AnalysisReport, type Certainty, type Severity } from "../api";
import { CertaintyBadge, SeverityBadge } from "../components/Badges";
import { Disclaimer, ErrorBanner, Spinner } from "../components/Feedback";
import { saveExplanations, useExplanation } from "../explanations";
import { DEFAULT_FILTER, filterFindings, groupByCategory, location, shortPath, type FindingFilter } from "../report";
import { href } from "../router";

const MAX_AI_SELECTION = 5;

export function Results({ id }: { id: string }) {
  const [report, setReport] = useState<AnalysisReport | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [filter, setFilter] = useState<FindingFilter>(DEFAULT_FILTER);
  const [selected, setSelected] = useState<string[]>([]);
  const [aiBusy, setAiBusy] = useState(false);
  const [aiError, setAiError] = useState<unknown>(null);
  const [aiNotice, setAiNotice] = useState<string | null>(null);

  useEffect(() => {
    setReport(null);
    setError(null);
    api.get(id).then(setReport, setError);
  }, [id]);

  const visible = useMemo(() => (report ? filterFindings(report.findings, filter) : []), [report, filter]);
  const groups = useMemo(() => groupByCategory(visible), [visible]);

  if (error) {
    return (
      <div className="page">
        <ErrorBanner error={error}>
          <p>
            <a href={href.analyze}>Run a new analysis</a>
          </p>
        </ErrorBanner>
      </div>
    );
  }
  if (!report) return <div className="page"><Spinner label="Loading report…" /></div>;

  const p = report.project;
  const total = report.findings.length;

  function toggleSeverity(s: Severity) {
    setFilter((f) => ({
      ...f,
      severities: f.severities.includes(s) ? f.severities.filter((x) => x !== s) : [...f.severities, s],
    }));
  }

  function toggleSelect(fid: string) {
    setSelected((cur) => (cur.includes(fid) ? cur.filter((x) => x !== fid) : cur.length >= MAX_AI_SELECTION ? cur : [...cur, fid]));
  }

  async function explainSelected() {
    setAiBusy(true);
    setAiError(null);
    try {
      const res = await api.explain(report!.analysisId, selected);
      saveExplanations(report!.analysisId, res.explanations);
      setAiNotice(`${res.explanations.length} explanation(s) from ${res.model}. ${res.notice}`);
      setSelected([]);
    } catch (e) {
      setAiError(e);
    } finally {
      setAiBusy(false);
    }
  }

  return (
    <div className="page">
      <div className="title-row">
        <div>
          <h1>Migration report</h1>
          <p className="muted">
            {report.originalFilename} · {new Date(report.createdAt).toLocaleString()} · target Java {report.targetJavaVersion} /
            Spring Boot {report.targetSpringBoot}
          </p>
        </div>
        <div className="actions">
          <a className="button" href={api.reportJsonUrl(report.analysisId)} download>
            Download JSON
          </a>
          <a className="button" href={api.reportMarkdownUrl(report.analysisId)} download>
            Download Markdown
          </a>
        </div>
      </div>

      <section className="grid4">
        <div className="card stat">
          <span className="label">Project</span>
          <span className="value small-value">{p.artifactId ?? "unknown"}</span>
          <span className="muted small">
            {p.groupId ?? "?"} · {p.version ?? "?"}
          </span>
        </div>
        <div className="card stat">
          <span className="label">Java</span>
          <span className="value">{p.javaVersion ?? "?"}</span>
          <span className="muted small" title={p.javaVersionSource ?? ""}>
            {p.javaVersionSource ? shortPath(p.javaVersionSource) : "not declared"}
          </span>
        </div>
        <div className="card stat">
          <span className="label">Spring Boot</span>
          <span className="value">{p.springBootVersion ?? "—"}</span>
          <span className="muted small" title={p.springBootVersionSource ?? ""}>
            {p.springBootVersionSource ? shortPath(p.springBootVersionSource) : "not detected"}
          </span>
        </div>
        <div className="card stat">
          <span className="label">Scanned</span>
          <span className="value">{p.javaFileCount}</span>
          <span className="muted small">
            Java files · {p.filesScanned} analyzed, {p.filesSkipped} skipped · {p.totalDependencies} deps
          </span>
        </div>
      </section>

      <section className="card">
        <h2>Findings by severity</h2>
        <div className="sev-bar" aria-label="Severity distribution">
          {SEVERITIES.map((s) =>
            report.countsBySeverity[s] ? (
              <span key={s} className={`seg sev-${s.toLowerCase()}`} style={{ flexGrow: report.countsBySeverity[s] }} />
            ) : null,
          )}
          {total === 0 && <span className="seg empty" style={{ flexGrow: 1 }} />}
        </div>
        <div className="sev-legend">
          {SEVERITIES.map((s) => (
            <button
              key={s}
              className={`chip ${filter.severities.includes(s) ? "on" : ""}`}
              onClick={() => toggleSeverity(s)}
              aria-pressed={filter.severities.includes(s)}
            >
              <SeverityBadge severity={s} /> {report.countsBySeverity[s] ?? 0}
            </button>
          ))}
          <span className="muted small">{total} total · click to filter</span>
        </div>
      </section>

      <div className="layout-2">
        <div>
          <section className="card">
            <div className="filters">
              <input
                type="search"
                placeholder="Filter by file, rule, text…"
                value={filter.query}
                onChange={(e) => setFilter({ ...filter, query: e.target.value })}
                aria-label="Filter findings"
              />
              <select
                value={filter.certainty}
                onChange={(e) => setFilter({ ...filter, certainty: e.target.value as Certainty | "ALL" })}
                aria-label="Certainty"
              >
                <option value="ALL">All certainty</option>
                <option value="VERIFIED">Verified only</option>
                <option value="POTENTIAL">Potential only</option>
              </select>
            </div>
            <div className="ai-bar">
              <span className="muted small">
                Select up to {MAX_AI_SELECTION} findings to explain with Claude ({selected.length} selected).
              </span>
              <button className="primary" disabled={selected.length === 0 || aiBusy} onClick={explainSelected}>
                {aiBusy ? "Asking Claude…" : "Explain selected"}
              </button>
            </div>
            {aiBusy && <Spinner label="Sending selected findings and redacted snippets to Claude…" />}
            {aiError != null && <ErrorBanner error={aiError} />}
            {aiNotice && <div className="banner ok">{aiNotice}</div>}
          </section>

          {total === 0 && (
            <section className="card">
              <h2>No findings</h2>
              <p>
                No rule matched. That is a good sign, but static analysis cannot prove a migration will succeed; run your
                test suite on the target versions.
              </p>
            </section>
          )}
          {total > 0 && visible.length === 0 && <p className="muted">No findings match the current filters.</p>}

          {groups.map(([category, list]) => (
            <section className="card group" key={category}>
              <h2>
                {CATEGORY_LABELS[category]} <span className="count">{list.length}</span>
              </h2>
              <ul className="finding-list">
                {list.map((f) => (
                  <FindingRow
                    key={f.id}
                    analysisId={report.analysisId}
                    finding={f}
                    selected={selected.includes(f.id)}
                    selectable={selected.includes(f.id) || selected.length < MAX_AI_SELECTION}
                    onToggle={() => toggleSelect(f.id)}
                  />
                ))}
              </ul>
            </section>
          ))}
        </div>

        <aside>
          <section className="card">
            <h2>Recommended steps</h2>
            <ol className="steps">
              {report.recommendedSteps.map((s, i) => (
                <li key={i}>{s}</li>
              ))}
            </ol>
          </section>
          {p.parseWarnings.length > 0 && (
            <section className="card">
              <h2>Analysis warnings</h2>
              <ul className="small">
                {p.parseWarnings.map((w, i) => (
                  <li key={i}>{w}</li>
                ))}
              </ul>
            </section>
          )}
          {p.keyDependencies.length > 0 && (
            <section className="card">
              <h2>Declared dependencies</h2>
              <table className="deps">
                <tbody>
                  {p.keyDependencies.map((d, i) => (
                    <tr key={i}>
                      <td>
                        <code>{d.artifactId}</code>
                        <div className="muted small">{d.groupId}</div>
                      </td>
                      <td className="small">{d.declaredVersion ?? <span className="muted">managed</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          )}
        </aside>
      </div>
      <Disclaimer text={report.disclaimer} />
    </div>
  );
}

function FindingRow(props: {
  analysisId: string;
  finding: import("../api").Finding;
  selected: boolean;
  selectable: boolean;
  onToggle: () => void;
}) {
  const { analysisId, finding: f } = props;
  const ai = useExplanation(analysisId, f.id);
  return (
    <li className="finding-row">
      <input
        type="checkbox"
        checked={props.selected}
        disabled={!props.selectable}
        onChange={props.onToggle}
        aria-label={`Select ${f.id} for AI explanation`}
      />
      <div className="finding-main">
        <a href={href.finding(analysisId, f.id)} className="finding-title">
          {f.title}
        </a>
        <div className="finding-meta">
          <SeverityBadge severity={f.severity} />
          <CertaintyBadge certainty={f.certainty} />
          <code className="small">{f.ruleId}</code>
          <span className="muted small mono">{shortPath(location(f))}</span>
          {ai && <span className="ai-tag">Claude ✓</span>}
        </div>
        <p className="evidence small">{f.evidence}</p>
      </div>
      <span className="muted small id">{f.id}</span>
    </li>
  );
}
