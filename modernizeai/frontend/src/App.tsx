import { useRoute, href } from "./router";
import { Home } from "./pages/Home";
import { Analyze } from "./pages/Analyze";
import { Results } from "./pages/Results";
import { FindingDetail } from "./pages/FindingDetail";
import { Setup } from "./pages/Setup";

export function App() {
  const route = useRoute();
  const active = (name: string) => (route.name === name ? "active" : "");
  return (
    <>
      <header className="topbar">
        <a className="brand" href={href.home}>
          <img src="/favicon.svg" alt="" width={22} height={22} />
          ModernizeAI
        </a>
        <nav>
          <a className={active("analyze")} href={href.analyze}>
            Analyze
          </a>
          <a className={active("setup")} href={href.setup}>
            Claude setup
          </a>
        </nav>
      </header>
      <main>
        {route.name === "home" && <Home />}
        {route.name === "analyze" && <Analyze />}
        {route.name === "results" && <Results id={route.id} />}
        {route.name === "finding" && <FindingDetail id={route.id} findingId={route.findingId} />}
        {route.name === "setup" && <Setup />}
        {route.name === "notFound" && (
          <div className="page">
            <h1>Page not found</h1>
            <a href={href.home}>Go home</a>
          </div>
        )}
      </main>
      <footer className="footer muted small">
        ModernizeAI MVP · static analysis plus optional Claude explanations · recommendations require human review
      </footer>
    </>
  );
}
