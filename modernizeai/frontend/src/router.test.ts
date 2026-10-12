import { describe, expect, it } from "vitest";
import { href, parseRoute } from "./router";

describe("parseRoute", () => {
  it("parses known routes", () => {
    expect(parseRoute("")).toEqual({ name: "home" });
    expect(parseRoute("#/analyze")).toEqual({ name: "analyze" });
    expect(parseRoute("#/results/abc")).toEqual({ name: "results", id: "abc" });
    expect(parseRoute(href.finding("abc", "F-001"))).toEqual({ name: "finding", id: "abc", findingId: "F-001" });
    expect(parseRoute("#/nope/x/y")).toEqual({ name: "notFound" });
  });
});
