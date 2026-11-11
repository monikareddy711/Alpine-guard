import { defineConfig } from '@playwright/test';

const BACKEND_JAR = 'backend/target/alpineguard-backend-1.0.0.jar';
const BACKEND_PORT = process.env.ALPINEGUARD_BACKEND_PORT ?? '8080';
const API_TARGET = `http://localhost:${BACKEND_PORT}`;

export default defineConfig({
  testDir: './e2e',
  timeout: 180_000,
  expect: { timeout: 30_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['json', { outputFile: 'e2e/reports/playwright-results.json' }]],
  use: {
    baseURL: 'http://localhost:4200',
    viewport: { width: 1440, height: 900 },
    screenshot: 'off',
    trace: 'off',
    launchOptions: {
      // PLAYWRIGHT_CHROME_PATH points at a system Chrome build when the Playwright browser
      // download is not available. Cesium needs WebGL, which headless containers only expose
      // through the software rasteriser.
      executablePath: process.env.PLAYWRIGHT_CHROME_PATH || undefined,
      args: ['--no-sandbox', '--enable-unsafe-swiftshader', '--use-angle=swiftshader']
    }
  },
  webServer: [
    {
      command: `java -Xmx512m -jar ${BACKEND_JAR} --server.port=${BACKEND_PORT}`,
      url: `${API_TARGET}/actuator/health`,
      timeout: 180_000,
      reuseExistingServer: true,
      stdout: 'pipe',
      stderr: 'pipe'
    },
    {
      command: 'node e2e/serve-dist.mjs',
      url: 'http://localhost:4200',
      timeout: 60_000,
      reuseExistingServer: true,
      env: { API_TARGET }
    }
  ]
});
