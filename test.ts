import { ApplicationConfig, APP_INITIALIZER } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { initializeApp } from './app.initializer';

export const appConfig: ApplicationConfig = {
    providers: [
        provideHttpClient(),
        // Nowoczesny zapis w Angular 20:
        {
            provide: APP_INITIALIZER,
            useFactory: initializeApp,
            multi: true,
        },
    ],
};