import { useSyncExternalStore } from "react";
import type { AiExplanation } from "./api";

// Tiny shared store so the dashboard and the detail view see the same AI results without refetching.
const store = new Map<string, AiExplanation>();
const listeners = new Set<() => void>();
let version = 0;

const key = (analysisId: string, findingId: string) => `${analysisId}/${findingId}`;

export function saveExplanations(analysisId: string, list: AiExplanation[]) {
  list.forEach((e) => store.set(key(analysisId, e.findingId), e));
  version++;
  listeners.forEach((l) => l());
}

export function useExplanation(analysisId: string, findingId: string): AiExplanation | undefined {
  useSyncExternalStore(
    (l) => {
      listeners.add(l);
      return () => listeners.delete(l);
    },
    () => version,
  );
  return store.get(key(analysisId, findingId));
}
