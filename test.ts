import { test, expect } from '@playwright/test';

// 1. describe jest synchroniczny (BEZ async)
test.describe('Weryfikacja wyszukiwarki Google', () => {

    // 2. test jest asynchroniczny (Z async) i nie zawiera w sobie innych testów
    test('Powinien poprawnie otworzyć stronę Google i sprawdzić tytuł', async ({ page }) => {
        // Przejście na stronę
        await page.goto('https://www.google.com');

        // Sprawdzenie, czy tytuł strony to "Google"
        await expect(page).toHaveTitle('Google');
    });

});


import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  use: {
    headless: true,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});