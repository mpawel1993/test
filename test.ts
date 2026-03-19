import { ApplicationConfig, provideAppInitializer } from '@angular/core';
import { inject } from '@angular/core';
import { MyConfigService } from './services/my-config.service';

export const appConfig: ApplicationConfig = {
    providers: [
        // Zamiast { provide: APP_INITIALIZER, ... }
        provideAppInitializer(() => {
            const configService = inject(MyConfigService);
            return configService.loadRemoteData(); // Musi zwracać Promise
        })
    ]
};