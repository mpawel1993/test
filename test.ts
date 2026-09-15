const { test, expect } = require('@playwright/test');

test('podstawowy test - sprawdzenie tytułu i interakcji', async ({ page }) => {
  // 1. Wejdź na stronę
  await page.goto('https://example.com');

  // 2. Sprawdź, czy tytuł strony jest poprawny
  await expect(page).toHaveTitle(/Example Domain/);

  // 3. Sprawdź, czy nagłówek H1 zawiera odpowiedni tekst
  const header = page.locator('h1');
  await expect(header).toHaveText('Example Domain');

  // 4. Kliknij w link "More information..."
  await page.locator('a').click();

  // 5. Upewnij się, że po kliknięciu URL się zmienił
  await expect(page).toHaveURL(/iana.org/);
});