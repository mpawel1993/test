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

loadScript(path: string, document: Document): Promise<void> {
  return new Promise<void>((resolve, reject) => {
    let node = document.createElement('script');
    node.type = 'text/javascript';
    node.async = true;
    node.src = path;

    node.onload = function () {
      console.log('done');
      resolve();
    };

    node.onerror = function () {
      reject(new Error(`Failed to load script: ${path}`));
    };

    document.getElementsByTagName('head')[0].appendChild(node);
  });
}