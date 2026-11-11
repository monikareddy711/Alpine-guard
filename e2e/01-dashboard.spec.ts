import { expect, test } from '@playwright/test';

test.describe('dashboard journeys', () => {
  test('loads stations, terrain and telemetry together', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.mark')).toHaveText('AlpineGuard');
    await expect(page.locator('.station-grid tbody tr').first()).toBeVisible();
    await expect(page.locator('.cesium-widget canvas').first()).toBeVisible();
    await expect(page.locator('.chart-panel').first()).toBeVisible();
    const rows = await page.locator('.station-grid tbody tr').count();
    expect(rows).toBeGreaterThan(0);
  });

  test('station row selection refreshes the telemetry chart', async ({ page }) => {
    await page.goto('/');
    const title = page.locator('.chart-panel').first().locator('.ag-panel-title span').first();
    await expect(title).toContainText('telemetry');
    await page.locator('.station-grid tbody tr', { hasText: 'Kuehtai' }).first().click();
    await expect(title).toContainText('Kuehtai');
    await expect(page.locator('.cesium-widget canvas').first()).toBeVisible();
  });

  test('switching the time window reloads the hazard breakdown', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('.hazard-panel .p-datatable-tbody tr').first()).toBeVisible();
    await page.getByText('24 h', { exact: true }).click();
    await expect(page.locator('.hazard-panel .ag-panel-title')).toContainText('24 h window');
  });

  test('route selection updates the segment table', async ({ page }) => {
    await page.goto('/');
    await page.locator('.route-select').click();
    await page.locator('.p-dropdown-panel').waitFor({ state: 'visible' });
    await page.locator('.p-dropdown-item', { hasText: 'Sulzkogel' }).first().dispatchEvent('click');
    await expect(page.locator('.hazard-panel .route-name')).toContainText('Sulzkogel');
    await expect(page.locator('.hazard-panel .p-datatable-tbody tr').first()).toBeVisible();
  });
});
