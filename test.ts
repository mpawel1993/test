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


import { defineConfig } from '@playwright/test';

export default defineConfig({
  use: {
    headless: true,
    // Ścieżka do lokalnie zainstalowanego Chrome
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
  },
});


import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  // Katalog z testami
  testDir: './tests',

  use: {
    // 1. Wymuszenie trybu w tle dla wszystkich projektów
    headless: true,

    // 2. Wykonywanie zrzutów ekranu w przypadku błędu
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],

        // Ustawienia uruchamiania własnej binarki Firefoxa
        launchOptions: {
          // Ścieżka do Firefoxa w systemie Linux / kontenerze Docker
          // (Zmień na właściwą, jeśli u Was jest w innym miejscu, np. /usr/bin/firefox-esr)
          executablePath: '/usr/bin/firefox',
        },
      },
    },
  ],
});

import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  projects: [
    {
      name: 'firefox',
      use: {
        ...devices['Desktop Firefox'],
        headless: true,
        launchOptions: {
          executablePath: process.env.FIREFOX_BIN || '/usr/bin/firefox',
          args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
          ],
          env: {
            ...process.env,
            MOZ_DISABLE_CONTENT_SANDBOX: '1',
            MOZ_DISABLE_RDD_SANDBOX: '1',
            MOZ_DISABLE_GMP_SANDBOX: '1',
            MOZ_DISABLE_NPAPI_SANDBOX: '1',
          },
        },
      },
    },
  ],
});