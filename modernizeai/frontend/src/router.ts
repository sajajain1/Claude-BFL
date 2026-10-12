import { useEffect, useState } from "react";

// Hash routing keeps the app a single static index.html, so it can be served by Spring Boot or any static host.
export type Route =
  | { name: "home" }
  | { name: "analyze" }
  | { name: "results"; id: string }
  | { name: "finding"; id: string; findingId: string }
  | { name: "setup" }
  | { name: "notFound" };

export function parseRoute(hash: string): Route {
  const parts = hash.replace(/^#\/?/, "").split("/").filter(Boolean).map(decodeURIComponent);
  if (parts.length === 0) return { name: "home" };
  if (parts[0] === "analyze" && parts.length === 1) return { name: "analyze" };
  if (parts[0] === "setup" && parts.length === 1) return { name: "setup" };
  if (parts[0] === "results" && parts.length === 2) return { name: "results", id: parts[1] };
  if (parts[0] === "results" && parts.length === 4 && parts[2] === "findings") {
    return { name: "finding", id: parts[1], findingId: parts[3] };
  }
  return { name: "notFound" };
}

export const href = {
  home: "#/",
  analyze: "#/analyze",
  setup: "#/setup",
  results: (id: string) => `#/results/${encodeURIComponent(id)}`,
  finding: (id: string, findingId: string) => `#/results/${encodeURIComponent(id)}/findings/${encodeURIComponent(findingId)}`,
};

export function navigate(to: string) {
  window.location.hash = to;
}

export function useRoute(): Route {
  const [route, setRoute] = useState(() => parseRoute(window.location.hash));
  useEffect(() => {
    const onChange = () => {
      setRoute(parseRoute(window.location.hash));
      window.scrollTo(0, 0);
    };
    window.addEventListener("hashchange", onChange);
    return () => window.removeEventListener("hashchange", onChange);
  }, []);
  return route;
}
