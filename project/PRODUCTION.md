# Production Readiness

## Security model
- **Authentication**: BCrypt(12) passwords; 15-minute access JWT (stored in `localStorage` as the SRS requires; the decoded user lives in `UserStoreService`); rotating opaque refresh token in an HttpOnly, SameSite=Strict cookie (only its SHA-256 hash is stored; reuse detection revokes the session).
- **Authorization**: roles ADMIN / CUSTOMER on every endpoint; ownership checks (`AccessService`) prevent a user reading or changing another user's cart, orders or profile (IDOR).
- **Admin signup** requires the `X-Admin-Code` header matching `ADMIN_INVITE_CODE`.
- **Guards (frontend)**: `AuthGuard` restores the session on reload and checks `data.roles` per route; the interceptor adds the token and retries once after a 401 via refresh.
- **Hardening**: security headers + CSP, explicit CORS origins, request-id logging, JSON 401/403 handlers, validation on all input.

## Note on token storage
The SRS requires the JWT in `localStorage`, so the frontend stores it there (plus a refresh cookie that renews it). This is simple but readable by scripts if the site ever has an XSS bug. The strictest option (token only in memory) is easy to switch back to in `auth.service.ts` if the SRS tests allow it.

## Limits
| Limit | Value |
|---|---|
| Requests per IP | 120/min global, 10/min on auth endpoints |
| Login lockout | 5 failures → 15 min (per email + IP) |
| AI search | 6/min and 60/day per user, query ≤ 200 chars |
| Gemini budget | 2000 embedding calls/day, circuit breaker, query cache; lexical fallback if Gemini is off |

## Environment variables (prod profile fails fast if missing)
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (≥ 32 chars, random), `ADMIN_INVITE_CODE`, `CORS_ORIGINS`, `GEMINI_API_KEY` (optional). Set them as OS environment variables.

## Deploy
Run the backend jar with `SPRING_PROFILES_ACTIVE=prod` and the env vars above; serve the Angular `dist` folder from any static host. Use HTTPS (cookie `secure=true` in prod).

## Verification status (honest)
- Frontend (Angular Material UI): production build succeeds with lazy chunks, 22 Karma unit tests pass, 60 Puppeteer end-to-end checks pass against a mock API (all pages, guards, dialogs, validations, search, filter, paging, profile, order details).
- Backend: written and unit tests included, but **not compiled or run in the build sandbox** (Maven Central was blocked). Run `mvn clean verify` first and fix any small compile issue.

## Known gaps / next steps
- Rate limiter and login lockout are in memory → single instance only (use Redis to scale).
- Schema via Hibernate `ddl-auto`; add Flyway migrations before real data.
- Only `GET /api/course/search` is paged; the other list endpoints still return everything (the SRS expects plain lists).
- No email verification / password reset.
- No integration tests (Testcontainers) yet.
