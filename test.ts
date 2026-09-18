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