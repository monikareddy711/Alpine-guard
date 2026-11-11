import { expect, test } from '@playwright/test';

const VIEWPORT = { width: 1440, height: 900 };

test.describe('screenshot capture', () => {
  test('dashboard overview', async ({ page }) => {
    await page.setViewportSize(VIEWPORT);
    await page.goto('/');
    await expect(page.locator('.station-grid tbody tr').first()).toBeVisible();
    await expect(page.locator('.cesium-widget canvas').first()).toBeVisible();
    await page.waitForTimeout(6000);
    await page.screenshot({ path: 'docs/img/dashboard-overview.png' });
  });

  test('route hazard detail', async ({ page }) => {
    await page.setViewportSize(VIEWPORT);
    await page.goto('/');
    await page.locator('.route-select').click();
    await page.locator('.p-dropdown-panel').waitFor({ state: 'visible' });
    await page.locator('.p-dropdown-item', { hasText: 'Sulzkogel' }).first().dispatchEvent('click');
    await expect(page.locator('.hazard-panel .route-name')).toContainText('Sulzkogel');
    await expect(page.locator('.hazard-panel .p-datatable-tbody tr').first()).toBeVisible();
    await page.waitForTimeout(6000);
    await page.screenshot({ path: 'docs/img/route-hazard-detail.png' });
  });

  test('test and benchmark output', async ({ page }) => {
    await page.setViewportSize(VIEWPORT);
    await page.goto('/reports/test-run.html');
    await expect(page.locator('#report')).toBeVisible();
    await page.waitForTimeout(500);
    await page.screenshot({ path: 'docs/img/test-and-benchmark-output.png' });
  });
});
