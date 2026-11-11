import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AppComponent } from './app/app.component';

declare global {
  interface Window {
    CESIUM_BASE_URL?: string;
  }
}

window.CESIUM_BASE_URL = 'assets/cesium/';

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(withFetch()), provideAnimations()]
}).catch((error) => console.error(error));
