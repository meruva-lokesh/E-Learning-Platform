/**
 * Development settings. Change apiUrl if your Spring Boot backend runs somewhere else.
 * The production build replaces this file with environment.prod.ts (see angular.json "fileReplacements").
 */
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  // Only used for display; the Gemini key itself lives on the server (gemini.api.key) and is never sent to the browser.
  aiSearchEnabled: true
};
