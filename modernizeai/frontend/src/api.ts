// Types mirror the backend's JSON (com.modernizeai.model.*).

export type Severity = "HIGH" | "MEDIUM" | "LOW" | "INFO";
export type Certainty = "VERIFIED" | "POTENTIAL";
export type Category =
  | "JAKARTA_NAMESPACE"
  | "SPRING_BOOT_UPGRADE"
  | "JAVA_VERSION"
  | "SPRING_API"
  | "DEPENDENCY"
  | "CONFIGURATION"
  | "JDK_API";

export const SEVERITIES: Severity[] = ["HIGH", "MEDIUM", "LOW", "INFO"];

export const CATEGORY_LABELS: Record<Category, string> = {
  JAKARTA_NAMESPACE: "javax.* → jakarta.*",
  SPRING_BOOT_UPGRADE: "Spring Boot upgrade",
  JAVA_VERSION: "Java version",
  SPRING_API: "Deprecated / removed Spring API",
  DEPENDENCY: "Dependencies",
  CONFIGURATION: "Configuration",
  JDK_API: "JDK internal / removed API",
};

export interface Finding {
  id: string;
  ruleId: string;
  title: string;
  category: Category;
  severity: Severity;
  certainty: Certainty;
  filePath: string | null;
  line: number | null;
  evidence: string;
  snippet: string | null;
  explanation: string;
  suggestedNextStep: string;
  references: string[];
}

export interface DependencyInfo {
  groupId: string;
  artifactId: string;
  declaredVersion: string | null;
  resolvedVersion: string | null;
  scope: string | null;
  line: number | null;
}

export interface ProjectSummary {
  groupId: string | null;
  artifactId: string | null;
  version: string | null;
  javaVersion: string | null;
  javaVersionSource: string | null;
  springBootVersion: string | null;
  springBootVersionSource: string | null;
  javaFileCount: number;
  filesScanned: number;
  filesSkipped: number;
  parseWarnings: string[];
  keyDependencies: DependencyInfo[];
  totalDependencies: number;
}

export interface AnalysisReport {
  analysisId: string;
  createdAt: string;
  originalFilename: string;
  targetJavaVersion: number;
  targetSpringBoot: string;
  project: ProjectSummary;
  findings: Finding[];
  countsBySeverity: Record<Severity, number>;
  countsByCategory: Partial<Record<Category, number>>;
  recommendedSteps: string[];
  disclaimer: string;
}

export interface AiExplanation {
  findingId: string;
  whyItMayOccur: string;
  minimalFix: string;
  compatibilityNotes: string;
  missingContext: string[];
  confidence: "low" | "medium" | "high";
}

export interface ExplanationResponse {
  analysisId: string;
  model: string;
  explanations: AiExplanation[];
  notice: string;
}

export interface AiStatus {
  configured: boolean;
  model: string;
  message: string;
}

/** Error carrying the backend's stable error code. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string,
  ) {
    super(message);
  }
}

const BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? "";

export function apiUrl(path: string): string {
  return BASE + path;
}

export async function toApiError(res: Response): Promise<ApiError> {
  let code = "HTTP_" + res.status;
  let message = `Request failed with HTTP ${res.status}.`;
  try {
    const body = await res.json();
    if (body && typeof body.message === "string") message = body.message;
    if (body && typeof body.code === "string") code = body.code;
  } catch {
    if (res.status === 502 || res.status === 504) {
      message = "The backend is not reachable. Is it running on port 8080?";
      code = "BACKEND_UNREACHABLE";
    }
  }
  return new ApiError(message, res.status, code);
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(apiUrl(path), init);
  } catch {
    throw new ApiError("Cannot reach the ModernizeAI backend. Start it with `mvn spring-boot:run` in /backend.", 0, "BACKEND_UNREACHABLE");
  }
  if (!res.ok) throw await toApiError(res);
  return (await res.json()) as T;
}

export const api = {
  analyze(file: File, targetJavaVersion: number): Promise<AnalysisReport> {
    const form = new FormData();
    form.append("file", file);
    form.append("targetJavaVersion", String(targetJavaVersion));
    return request<AnalysisReport>("/api/analyses", { method: "POST", body: form });
  },
  get(id: string): Promise<AnalysisReport> {
    return request<AnalysisReport>(`/api/analyses/${encodeURIComponent(id)}`);
  },
  explain(id: string, findingIds: string[]): Promise<ExplanationResponse> {
    return request<ExplanationResponse>(`/api/analyses/${encodeURIComponent(id)}/explanations`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ findingIds }),
    });
  },
  aiStatus(): Promise<AiStatus> {
    return request<AiStatus>("/api/ai/status");
  },
  reportJsonUrl: (id: string) => apiUrl(`/api/analyses/${encodeURIComponent(id)}/report.json`),
  reportMarkdownUrl: (id: string) => apiUrl(`/api/analyses/${encodeURIComponent(id)}/report.md`),
};
