import type { ReactNode } from "react";
import { ApiError } from "../api";
import { href } from "../router";

export function Spinner({ label }: { label: string }) {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      {label}
    </div>
  );
}

export function ErrorBanner({ error, children }: { error: unknown; children?: ReactNode }) {
  const err = error instanceof ApiError ? error : null;
  const message = err ? err.message : error instanceof Error ? error.message : String(error);
  return (
    <div className="banner error" role="alert">
      <strong>{titleFor(err?.code)}</strong>
      <p>{message}</p>
      {err?.code?.startsWith("AI_") && (
        <p>
          See <a href={href.setup}>Claude setup</a>.
        </p>
      )}
      {children}
    </div>
  );
}

function titleFor(code?: string): string {
  switch (code) {
    case "UNSAFE_ARCHIVE":
      return "Archive rejected for safety";
    case "INVALID_UPLOAD":
    case "UPLOAD_TOO_LARGE":
      return "Upload not accepted";
    case "NOT_A_MAVEN_PROJECT":
      return "Not a Maven project";
    case "ANALYSIS_NOT_FOUND":
      return "Analysis not found";
    case "AI_NOT_CONFIGURED":
      return "AI explanations are not configured";
    case "BACKEND_UNREACHABLE":
      return "Backend not reachable";
    default:
      return code?.startsWith("AI_") ? "AI explanation unavailable" : "Something went wrong";
  }
}

export function Disclaimer({ text }: { text?: string }) {
  return (
    <aside className="banner note">
      <strong>Review required.</strong>{" "}
      {text ??
        "ModernizeAI performs static analysis without building or running your code. Findings and AI suggestions can be incomplete or wrong. Review every recommendation before changing code."}
    </aside>
  );
}
