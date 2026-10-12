import type { Category, Certainty, Finding, Severity } from "./api";
import { SEVERITIES } from "./api";

export interface FindingFilter {
  severities: Severity[];
  certainty: Certainty | "ALL";
  query: string;
}

export const DEFAULT_FILTER: FindingFilter = { severities: [...SEVERITIES], certainty: "ALL", query: "" };

export function filterFindings(findings: Finding[], f: FindingFilter): Finding[] {
  const q = f.query.trim().toLowerCase();
  return findings.filter(
    (x) =>
      f.severities.includes(x.severity) &&
      (f.certainty === "ALL" || x.certainty === f.certainty) &&
      (q === "" ||
        [x.title, x.ruleId, x.filePath ?? "", x.evidence, x.id].some((s) => s.toLowerCase().includes(q))),
  );
}

/** Order in which a migration usually tackles each category. */
export const CATEGORY_ORDER: Category[] = [
  "SPRING_BOOT_UPGRADE",
  "JAVA_VERSION",
  "JAKARTA_NAMESPACE",
  "SPRING_API",
  "DEPENDENCY",
  "CONFIGURATION",
  "JDK_API",
];

/** Groups findings by category, in migration order. */
export function groupByCategory(findings: Finding[]): [Category, Finding[]][] {
  const groups = new Map<Category, Finding[]>();
  for (const f of findings) {
    const list = groups.get(f.category) ?? [];
    list.push(f);
    groups.set(f.category, list);
  }
  return [...groups.entries()].sort((a, b) => CATEGORY_ORDER.indexOf(a[0]) - CATEGORY_ORDER.indexOf(b[0]));
}

export function location(f: Finding): string {
  if (!f.filePath) return "project";
  return f.line ? `${f.filePath}:${f.line}` : f.filePath;
}

/** Strips the archive's top-level folder for display, e.g. "legacy-shop/src/A.java" -> "src/A.java". */
export function shortPath(path: string | null): string {
  if (!path) return "";
  const i = path.indexOf("/");
  return i > 0 && path.slice(0, i) !== "src" ? path.slice(i + 1) : path;
}
