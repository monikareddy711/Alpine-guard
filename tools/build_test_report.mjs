// Renders the captured backend, frontend and end to end results plus the benchmark
// table into a single static page used for the test report screenshot.
import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'node:fs';

const read = (path, fallback) => (existsSync(path) ? readFileSync(path, 'utf8') : fallback);

const backendLog = read('e2e/reports/backend-tests.log', 'backend log missing');
const frontendLog = read('e2e/reports/frontend-tests.log', 'frontend log missing');
const benchmark = read('docs/spatial-query-comparison.md', 'benchmark report missing');

const backendLines = backendLog
  .split('\n')
  .filter((line) => line.includes('Tests run:') || line.includes('BUILD'))
  .slice(-14)
  .join('\n');

let playwrightRows = '';
let playwrightSummary = 'no end to end results recorded';
if (existsSync('e2e/reports/playwright-results.json')) {
  const results = JSON.parse(readFileSync('e2e/reports/playwright-results.json', 'utf8'));
  const suites = [];
  const walk = (suite) => {
    for (const spec of suite.specs ?? []) {
      for (const test of spec.tests ?? []) {
        suites.push({ title: spec.title, status: test.results?.[0]?.status ?? 'unknown', duration: test.results?.[0]?.duration ?? 0 });
      }
    }
    for (const child of suite.suites ?? []) {
      walk(child);
    }
  };
  for (const suite of results.suites ?? []) {
    walk(suite);
  }
  playwrightRows = suites
    .map((item) => `<tr><td>${item.title}</td><td class="ok">${item.status}</td><td>${(item.duration / 1000).toFixed(1)} s</td></tr>`)
    .join('');
  playwrightSummary = `${suites.filter((item) => item.status === 'passed').length} of ${suites.length} journeys passed`;
}

const benchmarkTable = benchmark
  .split('\n')
  .filter((line) => line.startsWith('|'))
  .join('\n');

const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8" />
<title>AlpineGuard test and benchmark run</title>
<style>
  body { background:#10151c; color:#d7dee7; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size:12px; margin:0; padding:18px 22px; }
  h1 { font-size:16px; margin:0 0 4px; font-family: Inter, system-ui, sans-serif; }
  h2 { font-size:12px; text-transform:uppercase; letter-spacing:0.06em; color:#8b98a8; margin:18px 0 6px; }
  pre { background:#161d26; border:1px solid #26313d; border-radius:6px; padding:10px 12px; margin:0; overflow:hidden; white-space:pre-wrap; }
  table { border-collapse:collapse; width:100%; background:#161d26; border:1px solid #26313d; border-radius:6px; }
  td, th { padding:5px 9px; border-bottom:1px solid #26313d; text-align:left; }
  th { color:#8b98a8; font-weight:600; text-transform:uppercase; font-size:11px; }
  .ok { color:#4caf7d; }
  .meta { color:#8b98a8; }
  .cols { display:grid; grid-template-columns: 1fr 1fr; gap:14px; }
</style>
</head>
<body>
  <div id="report">
    <h1>AlpineGuard test and benchmark run</h1>
    <div class="meta">JUnit 5 backend suite, Karma/Jasmine specs, Playwright journeys and the spatial proximity benchmark</div>

    <h2>Backend (mvn test)</h2>
    <pre>${backendLines.replace(/</g, '&lt;')}</pre>

    <h2>Frontend (ng test)</h2>
    <pre>${frontendLog.replace(/</g, '&lt;')}</pre>

    <div class="cols">
      <div>
        <h2>End to end (playwright) - ${playwrightSummary}</h2>
        <table><tr><th>journey</th><th>status</th><th>duration</th></tr>${playwrightRows}</table>
      </div>
      <div>
        <h2>Spatial proximity benchmark</h2>
        <table>${benchmarkTable.replace(/^\| --- .*$/gm, '')}</table>
      </div>
    </div>
  </div>
</body>
</html>
`;

mkdirSync('e2e/reports', { recursive: true });
writeFileSync('e2e/reports/test-run.html', html);
console.log('wrote e2e/reports/test-run.html');
