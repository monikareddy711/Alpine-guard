// Serves the built frontend and proxies /api to the Spring Boot service.
// Used by the Playwright webServer entries so the specs exercise the production bundle.
import http from 'node:http';
import { createReadStream, existsSync, statSync } from 'node:fs';
import { extname, join, normalize } from 'node:path';

const PORT = Number(process.env.SERVE_PORT ?? 4200);
const ROOT = new URL('../frontend/dist/alpineguard/browser', import.meta.url).pathname;
const REPORTS = new URL('./reports', import.meta.url).pathname;
const API_TARGET = process.env.API_TARGET ?? 'http://localhost:8080';

const TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.bin': 'application/octet-stream',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

function sendFile(res, path) {
  if (!existsSync(path) || !statSync(path).isFile()) {
    res.writeHead(404, { 'content-type': 'text/plain' });
    res.end('not found');
    return;
  }
  res.writeHead(200, { 'content-type': TYPES[extname(path)] ?? 'application/octet-stream' });
  createReadStream(path).pipe(res);
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url ?? '/', `http://localhost:${PORT}`);

  if (url.pathname.startsWith('/api/')) {
    const upstream = http.request(
      `${API_TARGET}${url.pathname}${url.search}`,
      { method: req.method, headers: { ...req.headers, host: 'localhost:8080' } },
      (proxied) => {
        res.writeHead(proxied.statusCode ?? 502, proxied.headers);
        proxied.pipe(res);
      }
    );
    upstream.on('error', () => {
      res.writeHead(502, { 'content-type': 'text/plain' });
      res.end('backend unavailable');
    });
    req.pipe(upstream);
    return;
  }

  if (url.pathname.startsWith('/reports/')) {
    sendFile(res, join(REPORTS, normalize(url.pathname.replace('/reports/', ''))));
    return;
  }

  const relative = url.pathname === '/' ? 'index.html' : url.pathname.replace(/^\//, '');
  const candidate = join(ROOT, normalize(relative));
  if (existsSync(candidate) && statSync(candidate).isFile()) {
    sendFile(res, candidate);
    return;
  }
  sendFile(res, join(ROOT, 'index.html'));
});

server.listen(PORT, () => console.log(`serving dist on http://localhost:${PORT}`));
