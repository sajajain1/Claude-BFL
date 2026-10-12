import type { AiExplanation } from "../api";

export function AiExplanationCard({ e, model }: { e: AiExplanation; model?: string }) {
  return (
    <section className="ai-card" aria-label="Claude explanation">
      <header>
        <span className="ai-tag">Claude</span>
        <span className={`badge conf-${e.confidence}`}>confidence: {e.confidence}</span>
        {model && <span className="muted small">{model}</span>}
      </header>
      <h4>Why it may occur</h4>
      <p className="prewrap">{e.whyItMayOccur}</p>
      <h4>Minimal fix</h4>
      <p className="prewrap">{e.minimalFix}</p>
      <h4>Compatibility considerations</h4>
      <p className="prewrap">{e.compatibilityNotes}</p>
      {e.missingContext.length > 0 && (
        <>
          <h4>Missing context</h4>
          <ul>
            {e.missingContext.map((m, i) => (
              <li key={i}>{m}</li>
            ))}
          </ul>
        </>
      )}
      <p className="muted small">AI-generated from limited snippets. Verify before applying.</p>
    </section>
  );
}
