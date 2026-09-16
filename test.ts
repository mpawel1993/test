import { chromium, Browser } from 'playwright';

(async (): Promise<void> => {
  try {
    const browser: Browser = await chromium.launch({
      // Wykrywa zainstalowanego w systemie Google Chrome
      channel: 'chrome',
      headless: true,

      // Jeśli channel nie zadziała z powodu restrykcji, odkomentuj i podaj ścieżkę:
      // executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    });

    console.log('Połączono z firmowym Chrome!');
    await browser.close();
  } catch (error) {
    console.error('Błąd uruchamiania przeglądarki:', error);
  }
})();



import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  use: {
    headless: true,
    // Globalne wskazanie na firmowy Chrome dla wszystkich testów
    channel: 'chrome',
  },
});