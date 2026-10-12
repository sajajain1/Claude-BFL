import { useRef, useState, type DragEvent } from "react";
import { api } from "../api";
import { Disclaimer, ErrorBanner, Spinner } from "../components/Feedback";
import { href, navigate } from "../router";

const MAX_MB = 20;

export function Analyze() {
  const [file, setFile] = useState<File | null>(null);
  const [target, setTarget] = useState(17);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [dragging, setDragging] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  function choose(f: File | undefined | null) {
    setError(null);
    if (!f) return;
    if (!f.name.toLowerCase().endsWith(".zip")) {
      setError(new Error("Please choose a .zip archive of a Maven project."));
      return;
    }
    if (f.size > MAX_MB * 1024 * 1024) {
      setError(new Error(`The file is larger than the ${MAX_MB} MB limit.`));
      return;
    }
    setFile(f);
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setDragging(false);
    choose(e.dataTransfer.files?.[0]);
  }

  async function submit() {
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const report = await api.analyze(file, target);
      navigate(href.results(report.analysisId));
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page narrow">
      <h1>Analyze a project</h1>
      <p className="lead">
        Upload a ZIP of a Maven project (the folder containing <code>pom.xml</code>). Multi-module projects work; parent
        POMs that are not in the ZIP cannot be resolved and are reported as such.
      </p>

      <div
        className={`dropzone ${dragging ? "dragging" : ""} ${file ? "has-file" : ""}`}
        onDragOver={(e) => {
          e.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        onClick={() => input.current?.click()}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && input.current?.click()}
        aria-label="Choose a ZIP file"
      >
        <input
          ref={input}
          type="file"
          accept=".zip,application/zip"
          hidden
          onChange={(e) => choose(e.target.files?.[0])}
        />
        {file ? (
          <>
            <strong>{file.name}</strong>
            <span className="muted">{(file.size / 1024).toFixed(0)} KB · click to change</span>
          </>
        ) : (
          <>
            <strong>Drop a .zip here or click to browse</strong>
            <span className="muted">Max {MAX_MB} MB. Build output (target/), .git and node_modules are ignored.</span>
          </>
        )}
      </div>

      <div className="form-row">
        <label htmlFor="target">Target Java version</label>
        <select id="target" value={target} onChange={(e) => setTarget(Number(e.target.value))} disabled={busy}>
          <option value={17}>Java 17 (Spring Boot 3 minimum)</option>
          <option value={21}>Java 21 (LTS)</option>
        </select>
        <span className="muted small">Target Spring Boot: 3.2.x</span>
      </div>

      <div className="actions">
        <button className="primary" onClick={submit} disabled={!file || busy}>
          {busy ? "Analyzing…" : "Run analysis"}
        </button>
      </div>

      {busy && <Spinner label="Reading the archive and running migration rules…" />}
      {error != null && <ErrorBanner error={error} />}

      <div className="card subtle">
        <h3>No project handy?</h3>
        <p>
          The repository includes <code>samples/legacy-shop.zip</code>, a deliberately outdated Spring Boot 2.5 / Java 8
          project, and <code>samples/modern-service.zip</code>, a clean Boot 3.2 project that should produce no findings.
        </p>
      </div>
      <Disclaimer />
    </div>
  );
}
