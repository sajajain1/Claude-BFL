import { describe, expect, it } from "vitest";
import type { Finding } from "./api";
import { DEFAULT_FILTER, filterFindings, groupByCategory, location, shortPath } from "./report";

const base: Finding = {
  id: "F-001",
  ruleId: "MIG-JAKARTA-001",
  title: "javax imports",
  category: "JAKARTA_NAMESPACE",
  severity: "HIGH",
  certainty: "VERIFIED",
  filePath: "shop/src/A.java",
  line: 4,
  evidence: "import javax.persistence.Entity",
  snippet: null,
  explanation: "",
  suggestedNextStep: "",
  references: [],
};
const findings: Finding[] = [
  base,
  { ...base, id: "F-002", category: "DEPENDENCY", severity: "LOW", certainty: "POTENTIAL", filePath: "shop/pom.xml", line: null, title: "dup" },
  { ...base, id: "F-003", category: "DEPENDENCY", severity: "MEDIUM", title: "springfox" },
  { ...base, id: "F-004", category: "SPRING_BOOT_UPGRADE", severity: "HIGH", title: "boot" },
];

describe("report helpers", () => {
  it("filters by severity, certainty and text", () => {
    expect(filterFindings(findings, DEFAULT_FILTER)).toHaveLength(4);
    expect(filterFindings(findings, { ...DEFAULT_FILTER, severities: ["HIGH"] }).map((f) => f.id)).toEqual(["F-001", "F-004"]);
    expect(filterFindings(findings, { ...DEFAULT_FILTER, certainty: "POTENTIAL" }).map((f) => f.id)).toEqual(["F-002"]);
    expect(filterFindings(findings, { ...DEFAULT_FILTER, query: "SPRINGFOX" }).map((f) => f.id)).toEqual(["F-003"]);
  });

  it("orders groups in migration order", () => {
    expect(groupByCategory(findings).map(([c, l]) => [c, l.length])).toEqual([
      ["SPRING_BOOT_UPGRADE", 1],
      ["JAKARTA_NAMESPACE", 1],
      ["DEPENDENCY", 2],
    ]);
  });

  it("formats locations", () => {
    expect(location(base)).toBe("shop/src/A.java:4");
    expect(location({ ...base, filePath: null })).toBe("project");
    expect(shortPath("shop/src/A.java")).toBe("src/A.java");
    expect(shortPath("src/A.java")).toBe("src/A.java");
    expect(shortPath("pom.xml")).toBe("pom.xml");
  });
});
