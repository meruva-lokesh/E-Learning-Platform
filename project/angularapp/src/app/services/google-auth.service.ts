import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

/**
 * Loads Google's "Sign in with Google" script and draws its button.
 * The button gives us a Google ID token ("credential"); LoginComponent sends it to the backend.
 * When environment.googleClientId is empty nothing is loaded and the button stays hidden.
 */
@Injectable({
  providedIn: 'root'
})
export class GoogleAuthService {
  private static readonly SCRIPT_URL = 'https://accounts.google.com/gsi/client';
  private scriptLoaded: Promise<void> | null = null;

  /** True when a Google client id is configured. */
  get enabled(): boolean {
    return !!environment.googleClientId;
  }

  private loadScript(): Promise<void> {
    if (!this.scriptLoaded) {
      this.scriptLoaded = new Promise<void>((resolve, reject) => {
        const script = document.createElement('script');
        script.src = GoogleAuthService.SCRIPT_URL;
        script.async = true;
        script.defer = true;
        script.onload = () => resolve();
        script.onerror = () => {
          this.scriptLoaded = null;   // allow a retry later
          reject(new Error('Could not load Google sign-in'));
        };
        document.head.appendChild(script);
      });
    }
    return this.scriptLoaded;
  }

  /** Draws the Google button inside "host" (text: "Sign in with Google" or "Sign up with Google"); onCredential gets the Google ID token. */
  async renderButton(host: HTMLElement, onCredential: (credential: string) => void,
                     text: 'signin_with' | 'signup_with' | 'continue_with' = 'signin_with'): Promise<void> {
    if (!this.enabled) {
      return;
    }
    await this.loadScript();
    const google = (window as any).google;
    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: { credential: string }) => onCredential(response.credential)
    });
    google.accounts.id.renderButton(host, {
      type: 'standard',
      theme: 'filled_black',
      size: 'large',
      shape: 'pill',
      text,
      width: Math.min(Math.max(host.clientWidth || 320, 200), 400)
    });
  }
}
