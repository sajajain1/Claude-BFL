// Builds the steering committee deck from the framework blueprint.
// Run: node build-deck.js  ->  Vendor-Assurance-Control-Tower-SteerCo.pptx

const pptxgen = require("pptxgenjs");

const INK       = "12242B";
const INK_SOFT  = "1C3540";
const PETROL    = "1F5D6B";
const PETROL_LT = "7FB3BD";
const PAPER     = "F1F3EE";
const WHITE     = "FFFFFF";
const MUTED     = "56635E";
const MUTED_LT  = "9FB0AC";
const RULE      = "D3D8CE";
const RED       = "8E2A2A";
const AMBER     = "B5642A";
const GREEN     = "3F6B52";

const HEAD = "Cambria";
const BODY = "Calibri";

const pres = new pptxgen();
pres.layout = "LAYOUT_WIDE";           // 13.333 x 7.5
pres.author = "Outsourcing Compliance";
pres.title  = "Vendor Assurance Control Tower";

const W = 13.333, H = 7.5, M = 0.6;

const shadow = () => ({ type: "outer", color: "8E9A94", blur: 9, offset: 1, angle: 90, opacity: 0.25 });

function lightSlide() {
  const s = pres.addSlide();
  s.background = { color: PAPER };
  return s;
}
function darkSlide() {
  const s = pres.addSlide();
  s.background = { color: INK };
  return s;
}

// repeated motif: a petrol disc carrying the section number, left of every title
function heading(s, num, title, sub) {
  s.addShape(pres.ShapeType.ellipse, {
    x: M, y: 0.45, w: 0.42, h: 0.42, fill: { color: PETROL }, line: { color: PETROL },
  });
  s.addText(String(num), {
    x: M, y: 0.45, w: 0.42, h: 0.42, align: "center", valign: "middle",
    fontFace: BODY, fontSize: 13, bold: true, color: WHITE, isTextBox: true, margin: 0,
  });
  s.addText(title, {
    x: 1.2, y: 0.4, w: 11.5, h: 0.55, fontFace: HEAD, fontSize: 31, bold: true,
    color: INK, isTextBox: true, margin: 0, valign: "middle",
  });
  if (sub) {
    s.addText(sub, {
      x: 1.2, y: 1.0, w: 11.3, h: 0.62, fontFace: BODY, fontSize: 14, color: MUTED,
      isTextBox: true, margin: 0,
    });
  }
}

function card(s, x, y, w, h, fill) {
  s.addShape(pres.ShapeType.roundRect, {
    x, y, w, h, rectRadius: 0.04,
    fill: { color: fill || WHITE }, line: { color: RULE, width: 0.75 }, shadow: shadow(),
  });
}

function label(s, x, y, w, text, color) {
  s.addText(text, {
    x, y, w, h: 0.26, fontFace: BODY, fontSize: 10.5, bold: true, charSpacing: 1.4,
    color: color || PETROL, isTextBox: true, margin: 0,
  });
}

function footnote(s, text) {
  s.addText(text, {
    x: M, y: 6.92, w: 12.1, h: 0.32, fontFace: BODY, fontSize: 10, color: MUTED,
    italic: true, isTextBox: true, margin: 0,
  });
}

/* ───────────────────────── 1 · Title ───────────────────────── */
{
  const s = darkSlide();
  s.addText("OUTSOURCING COMPLIANCE   ·   STEERING COMMITTEE", {
    x: M, y: 1.35, w: 11, h: 0.3, fontFace: BODY, fontSize: 12, bold: true,
    charSpacing: 2.2, color: PETROL_LT, isTextBox: true, margin: 0,
  });
  s.addText("Vendor Assurance\nControl Tower", {
    x: M, y: 1.85, w: 9.6, h: 2.1, fontFace: HEAD, fontSize: 46, bold: true,
    color: WHITE, lineSpacing: 52, isTextBox: true, margin: 0,
  });
  s.addText(
    "An AI-assisted quality assurance and oversight framework for a population of 250,000+ outsourced service providers",
    { x: M, y: 4.05, w: 8.6, h: 0.9, fontFace: BODY, fontSize: 16, color: MUTED_LT,
      isTextBox: true, margin: 0, lineSpacing: 24 }
  );

  const facts = [
    ["Design blueprint", "v0.9 — for review"],
    ["Date", "9 September 2026"],
    ["Decision sought", "Approve Wave 1"],
  ];
  facts.forEach(([k, v], i) => {
    const x = M + i * 3.4;
    s.addText(k.toUpperCase(), {
      x, y: 5.55, w: 3.1, h: 0.26, fontFace: BODY, fontSize: 10, bold: true,
      charSpacing: 1.6, color: PETROL_LT, isTextBox: true, margin: 0,
    });
    s.addText(v, {
      x, y: 5.85, w: 3.1, h: 0.36, fontFace: HEAD, fontSize: 16, bold: true,
      color: WHITE, isTextBox: true, margin: 0,
    });
  });
  s.addNotes("Framing: this is a coverage problem, not a speed problem. We are asking for approval of Wave 1 only — a 90-day, evidence-generating phase that ends in a defensible go/no-go.");
}

/* ───────────────────────── 2 · The problem ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "01", "The problem is coverage, not speed",
    "At current volumes, manual QA cannot reach the population — so most of the estate is never reviewed at all.");

  card(s, M, 1.75, 5.5, 3.15);
  s.addText("95%", {
    x: 0.95, y: 2.05, w: 4.8, h: 1.3, fontFace: HEAD, fontSize: 78, bold: true,
    color: RED, isTextBox: true, margin: 0, valign: "middle",
  });
  s.addText("of completed assessments receive no quality review today", {
    x: 0.95, y: 3.42, w: 4.8, h: 0.72, fontFace: BODY, fontSize: 16, color: INK,
    isTextBox: true, margin: 0, lineSpacing: 22,
  });
  s.addText("And the 5% we do review is selected by reviewer availability, not by risk.", {
    x: 0.95, y: 4.18, w: 4.8, h: 0.55, fontFace: BODY, fontSize: 12, color: MUTED,
    italic: true, isTextBox: true, margin: 0,
  });

  const stats = [
    ["~310,000", "assessments completed each year across audits, reviews, SLA checks and control testing"],
    ["~1,240", "assessments landing every business day"],
    ["145 FTE", "required to quality-review 100% of them manually, at 45 minutes each"],
  ];
  stats.forEach(([v, k], i) => {
    const y = 1.75 + i * 1.09;
    card(s, 6.5, y, 6.23, 0.97);
    s.addText(v, {
      x: 6.8, y: y + 0.06, w: 2.1, h: 0.85, fontFace: HEAD, fontSize: 26, bold: true,
      color: PETROL, isTextBox: true, margin: 0, valign: "middle",
    });
    s.addText(k, {
      x: 8.95, y: y + 0.08, w: 3.6, h: 0.82, fontFace: BODY, fontSize: 11.5, color: MUTED,
      isTextBox: true, margin: 0, valign: "middle", lineSpacing: 15,
    });
  });

  card(s, M, 5.12, 12.13, 1.5, "E6EDE8");
  label(s, 0.9, 5.32, 6, "WHAT THIS COSTS US");
  s.addText(
    "We cannot evidence to a supervisor that outsourcing assessments are subject to consistent quality control; systemic control gaps stay invisible because nobody sees across the population; and vendor deterioration is discovered at the next assessment rather than when it happens.",
    { x: 0.9, y: 5.62, w: 11.5, h: 0.85, fontFace: BODY, fontSize: 13, color: INK,
      isTextBox: true, margin: 0, lineSpacing: 18 }
  );
  footnote(s, "Volumes are a planning model built on stated assumptions, to be replaced with measured figures from our estate in week one.");
  s.addNotes("The 95% is the headline. We are not slow — we are blind across nineteen twentieths of the estate.");
}

/* ───────────────────────── 3 · What changes ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "02", "What we are proposing",
    "A machine opinion on every assessment; human judgment concentrated where it changes an outcome.");

  const cols = [
    {
      x: M, title: "TODAY", tint: "EDEAE6", accent: RED,
      rows: [
        "5% of assessments reviewed, chosen by availability",
        "Defects found only if a reviewer happens to look",
        "No view across the population — themes invisible",
        "Deterioration surfaces at the next assessment",
        "QA effort scales linearly with vendor count",
      ],
    },
    {
      x: 6.93, title: "PROPOSED", tint: "E4EDEC", accent: PETROL,
      rows: [
        "100% machine-reviewed; every assessment scored",
        "~12% flagged to humans, ranked by vendor risk",
        "Findings clustered into control gaps and trends",
        "Early-warning signals fire between assessments",
        "Effort scales with exceptions, not with population",
      ],
    },
  ];

  cols.forEach((c) => {
    card(s, c.x, 1.75, 5.8, 4.05, c.tint);
    s.addText(c.title, {
      x: c.x + 0.32, y: 1.95, w: 5, h: 0.3, fontFace: BODY, fontSize: 11, bold: true,
      charSpacing: 2, color: c.accent, isTextBox: true, margin: 0,
    });
    c.rows.forEach((r, i) => {
      const y = 2.4 + i * 0.66;
      s.addShape(pres.ShapeType.ellipse, {
        x: c.x + 0.34, y: y + 0.12, w: 0.13, h: 0.13,
        fill: { color: c.accent }, line: { color: c.accent },
      });
      s.addText(r, {
        x: c.x + 0.62, y: y, w: 5.0, h: 0.56, fontFace: BODY, fontSize: 13,
        color: INK, isTextBox: true, margin: 0, valign: "middle", lineSpacing: 17,
      });
    });
  });

  card(s, M, 6.0, 12.13, 0.78, INK);
  s.addText(
    "The machine produces observations.  Only a human produces findings.",
    { x: 0.9, y: 6.0, w: 11.5, h: 0.78, fontFace: HEAD, fontSize: 18, bold: true,
      color: WHITE, isTextBox: true, margin: 0, valign: "middle" }
  );
  s.addNotes("This distinction is the load-bearing wall of the design — every governance control rests on it.");
}

/* ───────────────────────── 4 · Triage funnel ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "03", "How the work is triaged",
    "Everything is screened by machine. Human attention is spent only where the machine cannot decide.");

  const rows = [
    { lbl: "Assessments submitted", val: "310,000 / yr", pct: 100, w: 10.4, c: PETROL,
      note: "Deterministic rules + language review on every one. No sampling at this stage." },
    { lbl: "Auto-accepted — quality score ≥ 85, no blocking rule", val: "272,800  ·  88%", pct: 88, w: 9.15, c: GREEN,
      note: "5% blind re-review by humans as a control on the model, not on the vendor." },
    { lbl: "Flagged for human disposition", val: "37,200  ·  12%", pct: 12, w: 2.9, c: AMBER,
      note: "Target flag rate. Below 6% the model under-detects; above 20% reviewers stop trusting it." },
    { lbl: "Escalated — policy breach, critical vendor, systemic signal", val: "2,600  ·  0.8%", pct: 0.8, w: 1.35, c: RED,
      note: "Senior review, four-eyes, committee visibility." },
  ];

  rows.forEach((r, i) => {
    const y = 1.8 + i * 1.2;
    s.addText(r.lbl, {
      x: M, y: y, w: 8.2, h: 0.3, fontFace: BODY, fontSize: 13.5, bold: true,
      color: INK, isTextBox: true, margin: 0,
    });
    s.addText(r.val, {
      x: 8.9, y: y, w: 3.8, h: 0.3, fontFace: BODY, fontSize: 13.5, color: MUTED,
      align: "right", isTextBox: true, margin: 0,
    });
    s.addShape(pres.ShapeType.rect, {
      x: M, y: y + 0.36, w: 12.13, h: 0.22, fill: { color: "E1E5DD" }, line: { color: "E1E5DD" },
    });
    s.addShape(pres.ShapeType.rect, {
      x: M, y: y + 0.36, w: r.w, h: 0.22, fill: { color: r.c }, line: { color: r.c },
    });
    s.addText(r.note, {
      x: M, y: y + 0.63, w: 11.9, h: 0.3, fontFace: BODY, fontSize: 11, color: MUTED,
      isTextBox: true, margin: 0,
    });
  });

  s.addText(
    "Reviewer effort: ~8,700 hours a year — about 5.5 FTE — for 100% coverage.",
    { x: M, y: 6.55, w: 12.1, h: 0.38, fontFace: HEAD, fontSize: 16, bold: true,
      color: PETROL, isTextBox: true, margin: 0 }
  );
  footnote(s, "Bar lengths are proportional within each row's own scale band; percentages are of total submitted volume.");
  s.addNotes("The whole business case depends on the flag rate holding near 12%. Section on prerequisites explains why form standardisation is not optional.");
}

/* ───────────────────────── 5 · The business case ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "04", "Same cost. Twenty times the coverage.",
    "The case for this programme is assurance coverage, not headcount reduction — and we should present it that way.");

  s.addChart(
    pres.ChartType.bar,
    [{
      name: "Reviewer FTE",
      labels: ["Today\n5% coverage", "Manual QA\n100% coverage", "AI-assisted\n100% coverage"],
      values: [7.3, 145, 7.5],
    }],
    {
      x: M, y: 1.85, w: 7.5, h: 4.3,
      barDir: "bar", barGapWidthPct: 55,
      chartColors: [MUTED, RED, PETROL],
      showTitle: true, title: "Reviewer FTE required", titleFontFace: BODY,
      titleFontSize: 13, titleColor: MUTED,
      showValue: true, dataLabelPosition: "outEnd", dataLabelColor: INK,
      dataLabelFontFace: BODY, dataLabelFontSize: 13, dataLabelFormatCode: "0.0",
      showLegend: false,
      catAxisLabelColor: INK, catAxisLabelFontFace: BODY, catAxisLabelFontSize: 12,
      valAxisLabelColor: MUTED, valAxisLabelFontFace: BODY, valAxisLabelFontSize: 10,
      valAxisMaxVal: 170,
      valGridLine: { color: "DFE3DB", size: 0.75 },
      catGridLine: { style: "none" },
      plotArea: { fill: { color: PAPER } },
    }
  );

  const notes = [
    ["Coverage", "5%  →  100%", "Every assessment gets a documented quality opinion."],
    ["Sampling", "Availability  →  Risk", "Human review is directed at the highest-risk cases."],
    ["Scaling", "Linear  →  Exception-based", "Adding vendors no longer adds proportional QA cost."],
  ];
  notes.forEach(([k, v, d], i) => {
    const y = 1.95 + i * 1.42;
    card(s, 8.45, y, 4.28, 1.22);
    label(s, 8.75, y + 0.16, 3.7, k.toUpperCase());
    s.addText(v, {
      x: 8.75, y: y + 0.4, w: 3.7, h: 0.32, fontFace: HEAD, fontSize: 17, bold: true,
      color: INK, isTextBox: true, margin: 0,
    });
    s.addText(d, {
      x: 8.75, y: y + 0.74, w: 3.75, h: 0.42, fontFace: BODY, fontSize: 11, color: MUTED,
      isTextBox: true, margin: 0, lineSpacing: 14,
    });
  });
  footnote(s, "Add ~2 FTE for model oversight, back-testing and thematic analysis. Figures follow the assumption set in the blueprint.");
  s.addNotes("If anyone asks for the savings number: there isn't one worth quoting. The value is 20x coverage at roughly the team we have.");
}

/* ───────────────────────── 6 · Operating model ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "05", "Target operating model",
    "A federated model with a central AI assurance capability, sitting inside the three lines of defence.");

  const lines = [
    { n: "1", t: "First line — business units", c: PETROL,
      d: "Complete assessments; remediate observations raised against their vendors; own evidence quality at source." },
    { n: "2", t: "Second line — QA, AI product, model risk", c: PETROL,
      d: "QA team disposes of every flag. A separate AI product team owns rules and prompts. Model Risk validates independently." },
    { n: "3", t: "Third line — internal audit", c: PETROL,
      d: "Audits the QA process and the AI control itself, including end-to-end testing of the audit trail." },
  ];
  lines.forEach((l, i) => {
    const y = 1.8 + i * 1.28;
    card(s, M, y, 7.9, 1.15);
    s.addShape(pres.ShapeType.ellipse, {
      x: 0.85, y: y + 0.33, w: 0.5, h: 0.5, fill: { color: l.c }, line: { color: l.c },
    });
    s.addText(l.n, {
      x: 0.85, y: y + 0.33, w: 0.5, h: 0.5, align: "center", valign: "middle",
      fontFace: BODY, fontSize: 15, bold: true, color: WHITE, isTextBox: true, margin: 0,
    });
    s.addText(l.t, {
      x: 1.55, y: y + 0.16, w: 6.7, h: 0.3, fontFace: HEAD, fontSize: 15, bold: true,
      color: INK, isTextBox: true, margin: 0,
    });
    s.addText(l.d, {
      x: 1.55, y: y + 0.48, w: 6.75, h: 0.6, fontFace: BODY, fontSize: 11.5, color: MUTED,
      isTextBox: true, margin: 0, lineSpacing: 15,
    });
  });

  card(s, 8.75, 1.8, 3.98, 3.91, INK);
  s.addText("SEPARATION OF DUTIES", {
    x: 9.05, y: 2.05, w: 3.4, h: 0.3, fontFace: BODY, fontSize: 10.5, bold: true,
    charSpacing: 1.6, color: PETROL_LT, isTextBox: true, margin: 0,
  });
  s.addText(
    [
      { text: "The team that builds the model must not dispose of its output.", options: { bullet: true, breakLine: true, paraSpaceAfter: 10 } },
      { text: "Neither may approve a change to the scoring weights.", options: { bullet: true, breakLine: true, paraSpaceAfter: 10 } },
      { text: "Weight changes go to this committee, with Model Risk sign-off — because re-weighting silently re-tiers thousands of vendors.", options: { bullet: true, breakLine: true, paraSpaceAfter: 10 } },
      { text: "A weight change is, in substance, a change to risk appetite.", options: { bullet: true } },
    ],
    { x: 9.05, y: 2.45, w: 3.4, h: 3.1, fontFace: BODY, fontSize: 12, color: WHITE,
      isTextBox: true, margin: 0, lineSpacing: 16 }
  );

  card(s, M, 5.85, 12.13, 0.95, "E6EDE8");
  s.addText("Governance owner: Outsourcing / Operational Risk Committee — sets risk appetite thresholds, tier definitions, scoring weights, and any change to how much the machine is allowed to decide.", {
    x: 0.9, y: 5.85, w: 11.5, h: 0.95, fontFace: BODY, fontSize: 12.5, color: INK,
    isTextBox: true, margin: 0, valign: "middle", lineSpacing: 17,
  });
  s.addNotes("Note the new team: AI Assurance Product, ring-fenced in 2LoD. Small — a handful of people — but it must not be the QA team itself.");
}

/* ───────────────────────── 7 · Autonomy ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "06", "How much the machine is allowed to decide",
    "Autonomy is granted per check and per vendor tier, and earned through measured precision — never assumed at launch.");

  const levels = [
    ["L0", "Shadow", "Scores everything; output visible only to the AI team. Minimum six weeks per new check.", MUTED],
    ["L1", "Advisory", "Observations shown to reviewers with citations. The reviewer decides independently.", PETROL],
    ["L2", "Routing", "Decides who reviews what, and auto-accepts clean low-tier work. Blocked for critical vendors.", PETROL],
    ["L3", "Auto-disposition", "Closes clean Tier 4/5 assessments, with 5% blind sampling as the control.", PETROL],
  ];
  levels.forEach(([code, name, desc, c], i) => {
    const x = M + i * 3.09;
    card(s, x, 1.8, 2.87, 2.35);
    s.addShape(pres.ShapeType.ellipse, {
      x: x + 0.28, y: 2.05, w: 0.56, h: 0.56, fill: { color: c }, line: { color: c },
    });
    s.addText(code, {
      x: x + 0.28, y: 2.05, w: 0.56, h: 0.56, align: "center", valign: "middle",
      fontFace: BODY, fontSize: 14, bold: true, color: WHITE, isTextBox: true, margin: 0,
    });
    s.addText(name, {
      x: x + 0.28, y: 2.72, w: 2.35, h: 0.32, fontFace: HEAD, fontSize: 15, bold: true,
      color: INK, isTextBox: true, margin: 0,
    });
    s.addText(desc, {
      x: x + 0.28, y: 3.06, w: 2.35, h: 0.95, fontFace: BODY, fontSize: 11, color: MUTED,
      isTextBox: true, margin: 0, lineSpacing: 14.5,
    });
    if (i < 3) {
      s.addText("→", {
        x: x + 2.87, y: 2.15, w: 0.22, h: 0.4, align: "center", fontFace: BODY,
        fontSize: 16, color: PETROL_LT, isTextBox: true, margin: 0,
      });
    }
  });

  card(s, M, 4.4, 12.13, 2.15, "F0E4E2");
  s.addText("HARD LIMITS — NO AUTONOMY LEVEL LIFTS THESE", {
    x: 0.92, y: 4.62, w: 8, h: 0.3, fontFace: BODY, fontSize: 11, bold: true,
    charSpacing: 1.6, color: RED, isTextBox: true, margin: 0,
  });
  const limits = [
    "Never downgrades a vendor's risk tier without human approval",
    "Never closes an adverse or breach observation autonomously",
    "Never auto-accepts a critical or material vendor assessment",
    "Never communicates with, suspends, or terminates a vendor",
  ];
  limits.forEach((t, i) => {
    const x = 0.92 + (i % 2) * 5.9;
    const y = 5.02 + Math.floor(i / 2) * 0.62;
    s.addShape(pres.ShapeType.ellipse, {
      x, y: y + 0.13, w: 0.14, h: 0.14, fill: { color: RED }, line: { color: RED },
    });
    s.addText(t, {
      x: x + 0.28, y, w: 5.4, h: 0.42, fontFace: BODY, fontSize: 12.5, color: INK,
      isTextBox: true, margin: 0, valign: "middle",
    });
  });
  footnote(s, "Adverse outcomes are always human-decided and always attributable to a named reviewer.");
  s.addNotes("Promotion from L1 to L2 requires 85% precision sustained over eight weeks, per check. Some checks will never be promoted — that is a correct outcome.");
}

/* ───────────────────────── 8 · Use cases ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "07", "Where AI is used — and where rules are better",
    "Roughly half of what we need is not a generative AI problem. Solving it with rules is cheaper, reproducible and far easier to defend.");

  const groups = [
    {
      x: M, title: "DETERMINISTIC RULES  ·  WAVE 1", c: PETROL, tint: WHITE,
      items: [
        "Missing fields and conditional-logic gaps",
        "Evidence presence, type match, validity window",
        "Contradictions across paired answers",
        "Duplicate text, copy-forward from last year,\nimplausible completion times",
      ],
      foot: "~180 versioned rules. 100% reproducible, individually testable, explainable to a supervisor line by line.",
    },
    {
      x: 6.93, title: "LANGUAGE MODELS  ·  WAVE 2–3", c: AMBER, tint: WHITE,
      items: [
        "Judging whether a justification is substantive",
        "Reading evidence: does the SOC 2 actually\nsupport the claim being made?",
        "Policy and regulatory conformance checking",
        "Clustering findings into control-gap themes",
      ],
      foot: "Reserved for the three things only a model can do: judge prose, read unstructured evidence, cluster by meaning.",
    },
  ];

  groups.forEach((g) => {
    card(s, g.x, 1.85, 5.8, 4.0, g.tint);
    s.addText(g.title, {
      x: g.x + 0.32, y: 2.05, w: 5.2, h: 0.3, fontFace: BODY, fontSize: 10.5, bold: true,
      charSpacing: 1.6, color: g.c, isTextBox: true, margin: 0,
    });
    g.items.forEach((it, i) => {
      const y = 2.48 + i * 0.68;
      s.addShape(pres.ShapeType.ellipse, {
        x: g.x + 0.34, y: y + 0.14, w: 0.13, h: 0.13, fill: { color: g.c }, line: { color: g.c },
      });
      s.addText(it, {
        x: g.x + 0.62, y, w: 5.0, h: 0.6, fontFace: BODY, fontSize: 12.5, color: INK,
        isTextBox: true, margin: 0, valign: "middle", lineSpacing: 16,
      });
    });
    s.addText(g.foot, {
      x: g.x + 0.32, y: 5.2, w: 5.15, h: 0.55, fontFace: BODY, fontSize: 10.5, color: MUTED,
      italic: true, isTextBox: true, margin: 0, lineSpacing: 14,
    });
  });

  card(s, M, 6.05, 12.13, 0.78, "E6EDE8");
  s.addText("The highest-value use case is upstream: guidance to the preparer while the assessment is being completed prevents the defect instead of detecting it.", {
    x: 0.9, y: 6.05, w: 11.5, h: 0.78, fontFace: BODY, fontSize: 13, bold: true,
    color: INK, isTextBox: true, margin: 0, valign: "middle",
  });
  s.addNotes("In-flight guidance also changes the politics: the AI helps the first line finish faster rather than marking their homework.");
}

/* ───────────────────────── 9 · Architecture ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "08", "Data architecture",
    "A lakehouse on Microsoft Fabric. The GRC platform stays the system of record; the lakehouse becomes the system of analysis.");

  const stages = [
    ["SOURCES", "GRC · contracts · spend\nevidence · incidents · SLA\npolicy · external signals"],
    ["LAKEHOUSE", "Bronze — raw, immutable\nSilver — conformed\nGold — scores and themes"],
    ["QA ENGINE", "Rules · document AI\nlanguage review\nscoring and routing"],
    ["DISPOSITION", "Reviewer workbench\nimmutable decision record\nwrite-back to GRC"],
    ["CONSUMPTION", "Dashboards\nearly warning\nregulatory reporting"],
  ];
  stages.forEach(([t, d], i) => {
    const x = M + i * 2.52;
    const isEngine = i === 2;
    card(s, x, 1.95, 2.28, 2.0, isEngine ? INK : WHITE);
    s.addText(t, {
      x: x + 0.2, y: 2.15, w: 1.95, h: 0.3, fontFace: BODY, fontSize: 10.5, bold: true,
      charSpacing: 1.4, color: isEngine ? PETROL_LT : PETROL, isTextBox: true, margin: 0,
    });
    s.addText(d, {
      x: x + 0.2, y: 2.5, w: 1.95, h: 1.3, fontFace: BODY, fontSize: 11,
      color: isEngine ? WHITE : MUTED, isTextBox: true, margin: 0, lineSpacing: 15,
    });
    if (i < 4) {
      s.addText("→", {
        x: x + 2.28, y: 2.7, w: 0.24, h: 0.4, align: "center", fontFace: BODY,
        fontSize: 16, color: PETROL, isTextBox: true, margin: 0,
      });
    }
  });
  s.addText("←  reviewer decisions feed back as the training signal", {
    x: M, y: 4.05, w: 12.13, h: 0.3, align: "center", fontFace: BODY, fontSize: 11.5,
    italic: true, color: PETROL, isTextBox: true, margin: 0,
  });

  s.addText("Three data problems that will decide the outcome", {
    x: M, y: 4.5, w: 11, h: 0.35, fontFace: HEAD, fontSize: 17, bold: true,
    color: INK, isTextBox: true, margin: 0,
  });
  const probs = [
    ["Vendor identity resolution", "A 250,000-row master will contain heavy duplication. Until it is resolved, concentration risk is understated and every analytic is wrong."],
    ["The clause library", "Policy and regulation decomposed into individually testable obligations, each dated. This is a compliance-authoring job, not an engineering one."],
    ["Question standardisation", "If one control is asked six ways across six templates, we cannot detect contradictions or cluster themes. Prerequisite, not a parallel workstream."],
  ];
  probs.forEach(([t, d], i) => {
    const x = M + i * 4.13;
    card(s, x, 4.95, 3.87, 1.8);
    s.addText(t, {
      x: x + 0.26, y: 5.12, w: 3.35, h: 0.3, fontFace: HEAD, fontSize: 13.5, bold: true,
      color: PETROL, isTextBox: true, margin: 0,
    });
    s.addText(d, {
      x: x + 0.26, y: 5.45, w: 3.4, h: 1.15, fontFace: BODY, fontSize: 11, color: MUTED,
      isTextBox: true, margin: 0, lineSpacing: 14.5,
    });
  });
  s.addNotes("Entity resolution is typically the largest single line in a programme like this. Budget for it properly — skipping it invalidates the analytics.");
}

/* ───────────────────────── 10 · Three scores ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "09", "Risk scoring: three scores, deliberately not merged",
    "A flawless assessment can describe a dangerous vendor. Conflating document quality with vendor risk is the most common failure in this design.");

  const scores = [
    { code: "AQS", name: "Assessment Quality Score", range: "0 – 100",
      d: "Did this assessment meet documentation standards? Six weighted dimensions: completeness, evidence integrity, justification quality, internal consistency, policy conformance, data hygiene.",
      band: "≥ 85 accept  ·  70–84 advisory  ·  55–69 rework  ·  < 55 reject", c: PETROL },
    { code: "VCR", name: "Vendor Composite Risk", range: "5 tiers",
      d: "Inherent risk from criticality, data sensitivity, concentration, sub-outsourcing depth and geography — reduced by demonstrated control effectiveness, then adjusted by live signals.",
      band: "Controls mitigate at most 60% — criticality stays visible in the residual", c: PETROL },
    { code: "EWI", name: "Early Warning Index", range: "velocity",
      d: "Deterioration is a rate, not a level. Eight signals z-scored against the vendor's own baseline and its peer group, firing on a two-of-eight rule to suppress noise.",
      band: "Threshold set to what the team can genuinely investigate — 40–60 open alerts", c: PETROL },
  ];
  scores.forEach((sc, i) => {
    const x = M + i * 4.13;
    card(s, x, 1.85, 3.87, 3.55);
    s.addShape(pres.ShapeType.roundRect, {
      x: x + 0.26, y: 2.08, w: 0.95, h: 0.42, rectRadius: 0.04,
      fill: { color: sc.c }, line: { color: sc.c },
    });
    s.addText(sc.code, {
      x: x + 0.26, y: 2.08, w: 0.95, h: 0.42, align: "center", valign: "middle",
      fontFace: BODY, fontSize: 13, bold: true, color: WHITE, isTextBox: true, margin: 0,
    });
    s.addText(sc.range, {
      x: x + 1.35, y: 2.08, w: 2.2, h: 0.42, valign: "middle", fontFace: BODY,
      fontSize: 11, color: MUTED, isTextBox: true, margin: 0,
    });
    s.addText(sc.name, {
      x: x + 0.26, y: 2.62, w: 3.4, h: 0.6, fontFace: HEAD, fontSize: 15, bold: true,
      color: INK, isTextBox: true, margin: 0, lineSpacing: 19,
    });
    s.addText(sc.d, {
      x: x + 0.26, y: 3.26, w: 3.4, h: 1.35, fontFace: BODY, fontSize: 11, color: MUTED,
      isTextBox: true, margin: 0, lineSpacing: 15,
    });
    s.addText(sc.band, {
      x: x + 0.26, y: 4.62, w: 3.4, h: 0.65, fontFace: BODY, fontSize: 10.5, bold: true,
      color: PETROL, isTextBox: true, margin: 0, lineSpacing: 14,
    });
  });

  card(s, M, 5.62, 12.13, 1.15, "E6EDE8");
  label(s, 0.9, 5.82, 8, "DELIBERATELY ABSENT: A SINGLE VENDOR HEALTH SCORE");
  s.addText("A composite-of-composites hides what moved it, and management will act on the number without asking. Every score also carries a confidence measure — low confidence on a low-risk vendor usually means nobody has looked, which is itself a warning signal.", {
    x: 0.9, y: 6.1, w: 11.5, h: 0.55, fontFace: BODY, fontSize: 12, color: INK,
    isTextBox: true, margin: 0, lineSpacing: 16,
  });
  s.addNotes("Blocking rules override the arithmetic: expired evidence on a critical control routes to a human regardless of a high AQS.");
}

/* ───────────────────────── 11 · Worked example ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "10", "What a scored assessment actually looks like",
    "Mid-tier payment reconciliation provider · material vendor · annual reassessment.");

  const rows = [
    [{ text: "Dimension", options: { bold: true } }, { text: "What the engine found", options: { bold: true } }, { text: "Deduction", options: { bold: true, align: "right" } }],
    ["Completeness", "4 of 62 conditional fields left blank", "−7"],
    ["Evidence integrity", "ISO 27001 certificate expired three months ago", "−12"],
    ["Justification quality", "9 of 40 free-text answers fail 3 or more rubric criteria", "−9"],
    ["Internal consistency", "“No sub-outsourcing” contradicts a named fourth party elsewhere", "−8"],
    ["Policy conformance", "Business continuity test evidence absent (clause OPS-14)", "−6"],
    ["Data hygiene", "11 answers identical to last year's submission", "−3"],
  ];

  s.addTable(rows, {
    x: M, y: 1.85, w: 8.3,
    colW: [2.0, 5.05, 1.25],
    fontFace: BODY, fontSize: 12, color: INK,
    border: { type: "solid", color: RULE, pt: 0.75 },
    fill: { color: WHITE },
    rowH: 0.42, valign: "middle",
    margin: [4, 8, 4, 8],
  });

  card(s, 9.15, 1.85, 3.58, 2.05, INK);
  s.addText("SCORE", {
    x: 9.45, y: 2.05, w: 3, h: 0.28, fontFace: BODY, fontSize: 10.5, bold: true,
    charSpacing: 1.6, color: PETROL_LT, isTextBox: true, margin: 0,
  });
  s.addText("55", {
    x: 9.45, y: 2.28, w: 3, h: 1.05, fontFace: HEAD, fontSize: 56, bold: true,
    color: WHITE, isTextBox: true, margin: 0, valign: "middle",
  });
  s.addText("Rework band  ·  6 observations raised", {
    x: 9.45, y: 3.35, w: 3, h: 0.4, fontFace: BODY, fontSize: 12, color: PETROL_LT,
    isTextBox: true, margin: 0,
  });

  card(s, 9.15, 4.05, 3.58, 2.28, "F0E4E2");
  s.addText("BLOCKING RULE FIRED", {
    x: 9.45, y: 4.25, w: 3, h: 0.28, fontFace: BODY, fontSize: 10.5, bold: true,
    charSpacing: 1.6, color: RED, isTextBox: true, margin: 0,
  });
  s.addText("Expired evidence on a critical control routes straight to the escalation queue — not the rework queue — regardless of the score.\n\nDisposition required within 5 business days, by a named reviewer.", {
    x: 9.45, y: 4.58, w: 3.0, h: 1.6, fontFace: BODY, fontSize: 11.5, color: INK,
    isTextBox: true, margin: 0, lineSpacing: 15,
  });

  s.addText("Every observation carries the question, a verbatim quote, the policy clause it was judged against, the rubric criterion that failed, and the model and prompt versions used.", {
    x: M, y: 6.5, w: 12.13, h: 0.45, fontFace: BODY, fontSize: 12, bold: true,
    color: PETROL, isTextBox: true, margin: 0, lineSpacing: 16,
  });
  s.addNotes("An observation without a citation trail cannot be defended in an audit or contested by a business unit — we discard it rather than show it.");
}

/* ───────────────────────── 12 · Dashboards ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "11", "What management sees",
    "Four surfaces for four decisions — built in this order, because the reviewer workbench produces the data the other three display.");

  const surf = [
    ["Reviewer workbench", "2LoD reviewers", "What do I work on next, and what do I decide?"],
    ["Vendor 360", "Vendor owners", "What is true about this vendor, and what do I owe it?"],
    ["Business unit oversight", "BU heads, 1LoD risk", "Where is my portfolio weak, and who is overdue?"],
    ["Executive pack", "This committee", "Is the risk profile within appetite and improving?"],
  ];
  surf.forEach(([t, a, q], i) => {
    const y = 1.8 + i * 1.06;
    card(s, M, y, 6.6, 0.95);
    s.addText(t, {
      x: 0.85, y: y + 0.12, w: 3.0, h: 0.3, fontFace: HEAD, fontSize: 14, bold: true,
      color: INK, isTextBox: true, margin: 0,
    });
    s.addText(a, {
      x: 3.95, y: y + 0.14, w: 2.4, h: 0.28, fontFace: BODY, fontSize: 10.5, bold: true,
      color: PETROL, align: "right", isTextBox: true, margin: 0,
    });
    s.addText(q, {
      x: 0.85, y: y + 0.46, w: 5.5, h: 0.5, fontFace: BODY, fontSize: 11.5, color: MUTED,
      italic: true, isTextBox: true, margin: 0,
    });
  });

  // exception density heat grid
  card(s, 7.45, 1.8, 5.28, 3.35);
  s.addText("Exception density by business unit and vendor tier", {
    x: 7.72, y: 1.98, w: 4.8, h: 0.3, fontFace: BODY, fontSize: 11.5, bold: true,
    color: INK, isTextBox: true, margin: 0,
  });
  s.addText("Exceptions per 100 assessments — illustrative shape only", {
    x: 7.72, y: 2.24, w: 4.8, h: 0.26, fontFace: BODY, fontSize: 9.5, color: MUTED,
    italic: true, isTextBox: true, margin: 0,
  });
  const bus = ["Retail Lending", "SME & Commercial", "Payments & Cards", "Wealth & Distr.", "Technology"];
  const tiers = ["T1", "T2", "T3", "T4", "T5"];
  const grid = [
    [14, 21, 29, 18, 11],
    [19, 27, 38, 26, 16],
    [7, 12, 17, 13, 8],
    [11, 18, 22, 25, 15],
    [16, 20, 24, 19, 10],
  ];
  const ramp = ["E8ECE5", "EDD9C2", "E7BC9B", "D99878", "C06B58"];
  const tone = (v) => (v < 10 ? 0 : v < 15 ? 1 : v < 23 ? 2 : v < 31 ? 3 : 4);
  const gx = 9.35, gy = 2.85, cw = 0.62, ch = 0.4;
  tiers.forEach((t, c) => {
    s.addText(t, {
      x: gx + c * cw, y: gy - 0.28, w: cw, h: 0.26, align: "center", fontFace: BODY,
      fontSize: 9.5, bold: true, color: MUTED, isTextBox: true, margin: 0,
    });
  });
  bus.forEach((b, r) => {
    s.addText(b, {
      x: 7.72, y: gy + r * ch, w: 1.55, h: ch, valign: "middle", align: "right",
      fontFace: BODY, fontSize: 9.5, color: INK, isTextBox: true, margin: 0,
    });
    grid[r].forEach((v, c) => {
      s.addShape(pres.ShapeType.rect, {
        x: gx + c * cw, y: gy + r * ch, w: cw - 0.03, h: ch - 0.03,
        fill: { color: ramp[tone(v)] }, line: { color: WHITE, width: 1 },
      });
      s.addText(String(v), {
        x: gx + c * cw, y: gy + r * ch, w: cw - 0.03, h: ch - 0.03, align: "center",
        valign: "middle", fontFace: BODY, fontSize: 10, color: "231A17",
        isTextBox: true, margin: 0,
      });
    });
  });
  s.addText("Counts tell us which unit is large. Rates tell us which has a problem.", {
    x: 7.72, y: 4.78, w: 4.8, h: 0.3, fontFace: BODY, fontSize: 10.5, bold: true,
    color: PETROL, isTextBox: true, margin: 0,
  });

  card(s, M, 6.05, 12.13, 1.08, "E6EDE8");
  label(s, 0.9, 6.22, 8, "THE MEASURES THAT MATTER MOST TO THIS COMMITTEE");
  const kpis = "Tier 1 assessment currency (100%, any gap escalates)   ·   Model precision (≥ 85% of observations upheld)   ·   Blind-sample escape rate (< 2%)   ·   Reviewer override rate (< 15%)   ·   Overdue critical remediation (zero tolerance)   ·   Concentration exposure against board limit";
  s.addText(kpis, {
    x: 0.9, y: 6.5, w: 11.5, h: 0.5, fontFace: BODY, fontSize: 11.5, color: INK,
    isTextBox: true, margin: 0, lineSpacing: 16,
  });
  s.addNotes("Every measure has a named owner, a threshold and an escalation path. A measure without a threshold is a chart, not a control.");
}

/* ───────────────────────── 13 · Roadmap ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "12", "First 90 days",
    "Sequenced to produce a defensible go/no-go on real data — not a proof of concept that impresses a committee and then dies on contact with the vendor master.");

  const phases = [
    { n: "30", t: "Establish ground truth", c: PETROL,
      items: [
        "400 historical assessments QA'd independently by two senior reviewers — the golden dataset",
        "Measure where those two disagree; resolve into written rules",
        "Profile the vendor master: duplication, tiers, completeness",
        "Map assessment questions to one control taxonomy",
        "Open the model risk governance intake",
      ],
      out: "Baseline defect rates, reviewer agreement, data quality report" },
    { n: "60", t: "Rules engine in shadow", c: PETROL,
      items: [
        "Build ~180 versioned deterministic rules",
        "Run silently over 90 days of backlog; measure precision per rule",
        "Retire any rule below 70% precision rather than defend it",
        "Reviewer workbench in daily use by two reviewers",
        "Publish the quality-score distribution",
      ],
      out: "Shadow precision report and go/no-go on the generative wave" },
    { n: "90", t: "One segment live", c: PETROL,
      items: [
        "Add justification scoring and evidence extraction",
        "Full decision record into immutable storage",
        "Go live in advisory mode for one business unit and one assessment type",
        "Model risk validation pack submitted",
        "First executive dashboard to this committee",
      ],
      out: "Production advisory QA on a live segment, validated" },
  ];

  phases.forEach((p, i) => {
    const x = M + i * 4.13;
    card(s, x, 1.9, 3.87, 4.5);
    s.addShape(pres.ShapeType.ellipse, {
      x: x + 0.28, y: 2.12, w: 0.66, h: 0.66, fill: { color: p.c }, line: { color: p.c },
    });
    s.addText(p.n, {
      x: x + 0.28, y: 2.12, w: 0.66, h: 0.66, align: "center", valign: "middle",
      fontFace: BODY, fontSize: 16, bold: true, color: WHITE, isTextBox: true, margin: 0,
    });
    s.addText("DAYS", {
      x: x + 1.05, y: 2.18, w: 1.2, h: 0.25, fontFace: BODY, fontSize: 9, bold: true,
      charSpacing: 1.4, color: MUTED, isTextBox: true, margin: 0,
    });
    s.addText(p.t, {
      x: x + 1.05, y: 2.4, w: 2.6, h: 0.35, fontFace: HEAD, fontSize: 14, bold: true,
      color: INK, isTextBox: true, margin: 0,
    });
    p.items.forEach((it, j) => {
      const y = 2.94 + j * 0.56;
      s.addShape(pres.ShapeType.ellipse, {
        x: x + 0.3, y: y + 0.13, w: 0.11, h: 0.11, fill: { color: p.c }, line: { color: p.c },
      });
      s.addText(it, {
        x: x + 0.54, y, w: 3.15, h: 0.56, fontFace: BODY, fontSize: 10.5, color: MUTED,
        isTextBox: true, margin: 0, valign: "middle", lineSpacing: 13.5,
      });
    });
    s.addText(p.out, {
      x: x + 0.3, y: 5.82, w: 3.35, h: 0.5, fontFace: BODY, fontSize: 10.5, bold: true,
      color: PETROL, isTextBox: true, margin: 0, lineSpacing: 14,
    });
  });

  s.addText("If we do only one thing first: build the golden dataset. It needs no technology decision, no procurement and no approval — and without it we cannot tell whether any of the rest works.", {
    x: M, y: 6.45, w: 12.13, h: 0.6, fontFace: BODY, fontSize: 13, bold: true,
    color: INK, isTextBox: true, margin: 0, lineSpacing: 17,
  });
  s.addNotes("Expect 15-25% disagreement between the two reviewers. Where they disagree, the standard is ambiguous — and no model beats an ambiguous standard.");
}

/* ───────────────────────── 14 · Long-term vision ───────────────────────── */
{
  const s = darkSlide();
  s.addShape(pres.ShapeType.ellipse, {
    x: M, y: 0.45, w: 0.42, h: 0.42, fill: { color: PETROL_LT }, line: { color: PETROL_LT },
  });
  s.addText("13", {
    x: M, y: 0.45, w: 0.42, h: 0.42, align: "center", valign: "middle",
    fontFace: BODY, fontSize: 13, bold: true, color: INK, isTextBox: true, margin: 0,
  });
  s.addText("Where this goes: months 12 to 24", {
    x: 1.2, y: 0.4, w: 11.5, h: 0.55, fontFace: HEAD, fontSize: 31, bold: true,
    color: WHITE, isTextBox: true, margin: 0, valign: "middle",
  });
  s.addText("The destination is not faster periodic assessment. It is retiring periodic assessment as the primary control for most of the population.", {
    x: 1.2, y: 1.0, w: 11.0, h: 0.62, fontFace: BODY, fontSize: 14, color: MUTED_LT,
    isTextBox: true, margin: 0,
  });

  const vis = [
    ["Risk-adaptive cadence", "Assessment frequency driven by vendor risk and early-warning signals rather than by the calendar. Stable low-risk vendors move to continuous monitoring with attestation only; deteriorating vendors are assessed on trigger. This is what makes 250,000 vendors genuinely manageable."],
    ["Nth-party visibility", "A supply-chain graph across the estate, with concentration and single-point-of-failure analysis at the level of the business function rather than the individual vendor."],
    ["Scenario capability", "Model the operational impact of a named critical provider failing, against defined impact tolerances — the question supervisors increasingly ask directly."],
    ["Reproducible regulatory register", "Generated from the lakehouse, versioned, and reproducible exactly as it stood on any past date."],
  ];
  vis.forEach(([t, d], i) => {
    const x = M + (i % 2) * 6.35;
    const y = 1.85 + Math.floor(i / 2) * 2.4;
    s.addShape(pres.ShapeType.roundRect, {
      x, y, w: 6.03, h: 2.15, rectRadius: 0.04,
      fill: { color: INK_SOFT }, line: { color: "2C4E59", width: 0.75 },
    });
    s.addText(t, {
      x: x + 0.32, y: y + 0.22, w: 5.4, h: 0.35, fontFace: HEAD, fontSize: 17, bold: true,
      color: PETROL_LT, isTextBox: true, margin: 0,
    });
    s.addText(d, {
      x: x + 0.32, y: y + 0.62, w: 5.4, h: 1.35, fontFace: BODY, fontSize: 12, color: WHITE,
      isTextBox: true, margin: 0, lineSpacing: 16.5,
    });
  });
  s.addText("Scalable compliance monitoring for a 250,000-entity population — the objective we set out to meet.", {
    x: M, y: 6.75, w: 12.13, h: 0.4, fontFace: BODY, fontSize: 12.5, italic: true,
    color: MUTED_LT, isTextBox: true, margin: 0,
  });
  s.addNotes("This is the vision slide. Do not let it become the ask — the ask is Wave 1 only.");
}

/* ───────────────────────── 15 · Microsoft stack ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "14", "Technology: a Microsoft-native build",
    "Nothing here is novel infrastructure. The stack is not the hard part of this programme — the data and the governance are.");

  const layers = [
    ["Data platform", "Microsoft Fabric — Data Factory, OneLake, Lakehouse, Warehouse, Direct Lake semantic models"],
    ["AI services", "Azure AI Foundry and Azure OpenAI · Document Intelligence for evidence · AI Search for clause retrieval · Content Safety"],
    ["Rules & orchestration", "Azure Functions and Durable Functions · Service Bus · Fabric notebooks · Data Activator for alerting"],
    ["Experience", "Power Apps reviewer workbench in Teams · Power Automate for rework and escalation · Power BI for all four dashboards"],
    ["Governance & security", "Microsoft Purview for catalog, lineage, labels and retention · Entra ID with privileged access management · Key Vault · Azure Monitor"],
  ];
  layers.forEach(([t, d], i) => {
    const y = 1.8 + i * 0.83;
    card(s, M, y, 8.35, 0.72);
    s.addText(t, {
      x: 0.85, y, w: 2.35, h: 0.72, valign: "middle", fontFace: HEAD, fontSize: 13,
      bold: true, color: PETROL, isTextBox: true, margin: 0,
    });
    s.addText(d, {
      x: 3.3, y, w: 5.4, h: 0.72, valign: "middle", fontFace: BODY, fontSize: 11,
      color: MUTED, isTextBox: true, margin: 0, lineSpacing: 14,
    });
  });

  card(s, 9.2, 1.8, 3.53, 3.75, INK);
  s.addText("WHERE COPILOT FITS", {
    x: 9.5, y: 2.0, w: 3.0, h: 0.3, fontFace: BODY, fontSize: 10.5, bold: true,
    charSpacing: 1.6, color: PETROL_LT, isTextBox: true, margin: 0,
  });
  s.addText(
    [
      { text: "Yes — helping a reviewer think: summarising vendor history, drafting committee narrative, answering policy questions with citations.", options: { bullet: true, breakLine: true, paraSpaceAfter: 12 } },
      { text: "No — as the control itself. A control must be deterministic in invocation, versioned and reproducible. An ad hoc chat session is none of those.", options: { bullet: true, breakLine: true, paraSpaceAfter: 12 } },
      { text: "The rule: Copilot helps a human think. The pipeline performs the control.", options: { bullet: true } },
    ],
    { x: 9.5, y: 2.4, w: 3.0, h: 2.9, fontFace: BODY, fontSize: 11.5, color: WHITE,
      isTextBox: true, margin: 0, lineSpacing: 15.5 }
  );

  card(s, M, 5.95, 12.13, 0.85, "E6EDE8");
  s.addText("Report cost per assessment as a monthly measure. It is the number that determines whether this scales to the full population or stalls at Wave 2.", {
    x: 0.9, y: 5.95, w: 11.5, h: 0.85, fontFace: BODY, fontSize: 12.5, color: INK,
    isTextBox: true, margin: 0, valign: "middle",
  });
  s.addNotes("Three cost levers: rules first so the model only sees answers needing language judgment; batch overnight on provisioned throughput; cache the clause corpus.");
}

/* ───────────────────────── 16 · Governance ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "15", "Governance, compliance and human review",
    "The supervisory question will not be “does your AI work?” It will be “how do you know, who checked, and can you show me a decision from eighteen months ago exactly as it was made?”");

  const left = [
    "Machine output is an observation; only a named human creates a finding",
    "Four-eyes on tier changes, critical findings and escalations",
    "Mandatory human review for every critical vendor and every policy-breach flag",
    "5% of auto-accepted work blind re-reviewed, escape rate reported here monthly",
    "Unconditional reviewer override rights, with a reason code",
    "Appeal path for business units, with a defined response SLA",
    "Automation-bias monitoring — a reviewer accepting 99% in eight seconds is a control failure",
  ];
  card(s, M, 1.95, 6.4, 4.3);
  label(s, 0.88, 2.15, 5.5, "HUMAN REVIEW CONTROLS");
  left.forEach((t, i) => {
    const y = 2.5 + i * 0.52;
    s.addShape(pres.ShapeType.ellipse, {
      x: 0.9, y: y + 0.11, w: 0.12, h: 0.12, fill: { color: PETROL }, line: { color: PETROL },
    });
    s.addText(t, {
      x: 1.14, y, w: 5.7, h: 0.46, fontFace: BODY, fontSize: 11.5, color: INK,
      isTextBox: true, margin: 0, valign: "middle", lineSpacing: 14.5,
    });
  });

  card(s, 7.25, 1.95, 5.48, 2.05);
  label(s, 7.53, 2.15, 4.8, "REGULATORY FRAME");
  s.addText("RBI outsourcing of IT services and financial services directions, and the IT governance direction · DPDP Act · CERT-In · EBA and DORA for EU-facing entities · model risk management expectations for the AI itself · EU AI Act classification where in scope.", {
    x: 7.53, y: 2.45, w: 4.9, h: 1.4, fontFace: BODY, fontSize: 11, color: MUTED,
    isTextBox: true, margin: 0, lineSpacing: 15,
  });

  card(s, 7.25, 4.15, 5.48, 2.1, "F0E4E2");
  s.addText("THE RECURSIVE POINT", {
    x: 7.53, y: 4.35, w: 4.8, h: 0.28, fontFace: BODY, fontSize: 10.5, bold: true,
    charSpacing: 1.6, color: RED, isTextBox: true, margin: 0,
  });
  s.addText("This platform is itself an outsourcing arrangement — cloud infrastructure and third-party model services supporting the outsourcing control function. It must go through our own framework, plausibly as a material arrangement, including exit strategy and audit rights.\n\nFailing to do so is a finding that writes itself.", {
    x: 7.53, y: 4.65, w: 4.9, h: 1.5, fontFace: BODY, fontSize: 11, color: INK,
    isTextBox: true, margin: 0, lineSpacing: 14.5,
  });

  s.addText("Test to run in year one: take a closed observation from six months earlier and reproduce its exact inputs, retrieved clauses and output. If we cannot, the audit trail is decorative.", {
    x: M, y: 6.45, w: 12.13, h: 0.5, fontFace: BODY, fontSize: 12.5, bold: true,
    color: PETROL, isTextBox: true, margin: 0, lineSpacing: 16,
  });
  s.addNotes("Also: fairness testing quarterly. A model penalising small or non-English vendors for prose quality rather than control quality is a real failure mode here.");
}

/* ───────────────────────── 17 · Risks ───────────────────────── */
{
  const s = lightSlide();
  heading(s, "16", "Where this fails",
    "None of these are reasons not to proceed. All of them are reasons to sequence it the way we have.");

  const risks = [
    ["Vendor master is worse than assumed", "Entity resolution finds heavy duplication", "Profiling in the first 30 days, before any build commitment"],
    ["The standard was never written down", "Two senior reviewers disagree on a third of the baseline", "Measure agreement first; write rules for the disagreements"],
    ["Flag rate too high and reviewers drown", "More than 20% flagged in shadow mode", "Precision gates per rule; retire weak rules; standardise forms in Wave 1"],
    ["Reviewers rubber-stamp the machine", "Acceptance above 95% with falling time per case", "Automation-bias monitoring as a named control; blind sampling"],
    ["Model risk approval arrives after build", "Validation requests land in month five", "Governance intake opens in week one, in parallel with data work"],
    ["Alert fatigue in early warning", "Open alert queue passes 200", "Two-of-eight firing rule; threshold set to team capacity; a quarter in shadow"],
  ];

  const hdr = ["Failure mode", "Early symptom", "What in this plan prevents it"];
  hdr.forEach((h, i) => {
    s.addText(h.toUpperCase(), {
      x: M + [0, 4.35, 8.1][i], y: 1.85, w: 4.0, h: 0.28, fontFace: BODY, fontSize: 10,
      bold: true, charSpacing: 1.4, color: PETROL, isTextBox: true, margin: 0,
    });
  });
  risks.forEach((r, i) => {
    const y = 2.25 + i * 0.76;
    card(s, M, y, 12.13, 0.66);
    s.addText(r[0], {
      x: 0.82, y, w: 3.9, h: 0.66, valign: "middle", fontFace: BODY, fontSize: 12,
      bold: true, color: INK, isTextBox: true, margin: 0, lineSpacing: 14.5,
    });
    s.addText(r[1], {
      x: 4.95, y, w: 3.55, h: 0.66, valign: "middle", fontFace: BODY, fontSize: 11,
      color: RED, isTextBox: true, margin: 0, lineSpacing: 14.5,
    });
    s.addText(r[2], {
      x: 8.7, y, w: 3.85, h: 0.66, valign: "middle", fontFace: BODY, fontSize: 11,
      color: MUTED, isTextBox: true, margin: 0, lineSpacing: 14.5,
    });
  });
  s.addNotes("Be candid here. A steering committee that hears only upside stops believing the rest of the pack.");
}

/* ───────────────────────── 18 · The ask ───────────────────────── */
{
  const s = darkSlide();
  s.addText("DECISIONS SOUGHT TODAY", {
    x: M, y: 0.75, w: 11, h: 0.3, fontFace: BODY, fontSize: 12, bold: true,
    charSpacing: 2.2, color: PETROL_LT, isTextBox: true, margin: 0,
  });
  s.addText("What we need from this committee", {
    x: M, y: 1.15, w: 11.5, h: 0.7, fontFace: HEAD, fontSize: 36, bold: true,
    color: WHITE, isTextBox: true, margin: 0,
  });

  const asks = [
    ["1", "Approve Wave 1", "Ninety days: golden dataset, data profiling, rules engine in shadow, reviewer workbench. Ends in an evidence-based go/no-go, not a commitment to scale."],
    ["2", "Endorse the operating principle", "The machine produces observations; only a human produces findings. Adverse outcomes stay human-decided and named."],
    ["3", "Name the governance owner", "This committee owns risk appetite thresholds, tier definitions, scoring weights, and every change to machine autonomy."],
    ["4", "Release the two prerequisites", "Reviewer time for the golden dataset, and a mandate to standardise and retire assessment templates across business units."],
  ];
  asks.forEach(([n, t, d], i) => {
    const y = 2.25 + i * 1.16;
    s.addShape(pres.ShapeType.ellipse, {
      x: M, y: y + 0.12, w: 0.52, h: 0.52, fill: { color: PETROL_LT }, line: { color: PETROL_LT },
    });
    s.addText(n, {
      x: M, y: y + 0.12, w: 0.52, h: 0.52, align: "center", valign: "middle",
      fontFace: BODY, fontSize: 15, bold: true, color: INK, isTextBox: true, margin: 0,
    });
    s.addText(t, {
      x: 1.3, y, w: 3.6, h: 0.4, fontFace: HEAD, fontSize: 17, bold: true,
      color: WHITE, isTextBox: true, margin: 0,
    });
    s.addText(d, {
      x: 1.3, y: 0.4 + y, w: 11.2, h: 0.62, fontFace: BODY, fontSize: 12.5, color: MUTED_LT,
      isTextBox: true, margin: 0, lineSpacing: 16.5,
    });
  });

  s.addText("Objective: reduce manual QA effort, improve assessment quality, identify systemic outsourcing risk early, and build a compliance monitoring capability that scales to the whole population.", {
    x: M, y: 6.85, w: 12.13, h: 0.45, fontFace: BODY, fontSize: 11.5, italic: true,
    color: PETROL_LT, isTextBox: true, margin: 0,
  });
  s.addNotes("Close on the ask. Wave 1 is deliberately small and reversible — the point is to generate evidence, not to commit to the full build.");
}

pres.writeFile({ fileName: "Vendor-Assurance-Control-Tower-SteerCo.pptx" })
  .then((f) => console.log("wrote " + f));
