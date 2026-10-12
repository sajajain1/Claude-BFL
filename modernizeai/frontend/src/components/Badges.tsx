import type { Certainty, Severity } from "../api";

export function SeverityBadge({ severity }: { severity: Severity }) {
  return <span className={`badge sev-${severity.toLowerCase()}`}>{severity}</span>;
}

export function CertaintyBadge({ certainty }: { certainty: Certainty }) {
  const tip =
    certainty === "VERIFIED"
      ? "Evidence was observed deterministically and the condition is documented as incompatible. Still requires review."
      : "A pattern matched that often needs attention, but context the scanner cannot see decides whether it is a problem.";
  return (
    <span className={`badge cert-${certainty.toLowerCase()}`} title={tip}>
      {certainty === "VERIFIED" ? "Verified" : "Potential"}
    </span>
  );
}
