# E-Learning Platform

Spring Boot 3.2.5 (Java 17, MySQL, JWT) backend + Angular 16 frontend with Angular Material.
Ports follow the SRS: **backend 8080, frontend 8081**, database `appdb` (user `root`, password `examly`).

See also: [TEAM_PLAN.md](TEAM_PLAN.md) (who does what, daily Git routine), [docs/GUIDELINES.md](docs/GUIDELINES.md) (Project Gladiator checklist, point by point), [PRODUCTION.md](PRODUCTION.md) (security, limits, status), [IMPROVEMENTS.md](IMPROVEMENTS.md) (what to add next).

## What you get
- **Backend**: layered Spring Boot (controller, service interface + impl, repository, DTO, exception hierarchy), JWT, Swagger, logging, validation, paging, dashboard statistics.
- **Frontend**: Angular 16 + Angular Material, lazy-loaded modules (auth, admin, customer, course details), a Udemy / Coursera-inspired layout with its own indigo and amber branding, Inter and Plus Jakarta Sans fonts.
  - Pages: login, register, home, dashboard with live numbers, catalog with keyword search, price filter, sorting, paging and AI search, course details with reviews, cart, payment, enrollments and order details, profile, review form, admin course, enrollment and review screens.

## Run it on your laptop

You need: Java 17, Maven 3.8+, Node 20 (`nvm use 20`), MySQL 8 running on port 3306.

### 1. Database
```
mysql -u root -p
```
Password: `examly` (if your MySQL root password is different, see "Different MySQL password" below). Then:
```
CREATE DATABASE IF NOT EXISTS appdb;
EXIT;
```
Tables are created automatically on first start.

### 2. Backend (terminal 1)
```
cd springapp
mvn clean install -DskipTests
mvn spring-boot:run
```
Wait for `Started SpringappApplication`. Check http://localhost:8080/api/health shows status UP.

### 3. Frontend (terminal 2)
```
cd angularapp
nvm use 20
npm install
npm start
```
Open http://localhost:8081 (the login page opens first).

### 4. Try the whole flow
1. Register an **Admin** (role Admin) and a **Customer**; each registration returns you to Login.
2. Admin: add 2-3 courses (Add Course), edit one, delete one.
3. Customer: log in, fill the details form (first login only), open Available Courses, try AI Search, add courses to the cart, Enroll Now, Make Payment, check My Enrollments, add a review.
4. Admin: check View Enrollments and View Reviews.
5. Logout asks "Are you sure"; typing /dashboard in the address bar while logged out sends you to Login.

### Different MySQL password
No file edits needed. Set an environment variable before starting the backend:
- Mac/Linux: `export DB_PASSWORD=yourpassword`
- Windows PowerShell: `$env:DB_PASSWORD="yourpassword"`

Other optional variables: `DB_URL`, `DB_USERNAME`, `JWT_SECRET`, `ADMIN_INVITE_CODE`, `CORS_ORIGINS`, `GEMINI_API_KEY`.

### AI Search (optional)
Works without any key (word-overlap fallback). For real semantic search set `GEMINI_API_KEY` before starting the backend.

## Frontend structure
```
angularapp/src/
  environments/        environment.ts (dev) and environment.prod.ts: API base URL
  theme.scss           Angular Material theme and fonts
  styles.css           shared design tokens and layout classes
  app/
    constant.ts        every API path, storage key and role name
    models/            interfaces for all data shapes
    services/          one service per API area, user-store.service (decoded JWT), auth.interceptor
    modules/           lazy feature modules: auth, admin, customer, course-details
    components/        pages, one folder each
    shared/            course card, star rating, shared Angular/Material imports
```
To point the app at another server, change `apiUrl` in the environment files only.

## Run the tests
```
cd springapp && mvn clean verify
cd angularapp && npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox
```

## API (matches the SRS table)
`POST /api/register`, `POST /api/login`, `/api/course` (+ `/api/course/courses/{id}`), `/api/customer`, `/api/order`, `/api/review`, `/api/cart`, `POST /api/course-ai/search`.
Extra: `POST /api/refresh`, `POST /api/logout`, `GET /api/health`.

New endpoints (each also documented in Swagger UI at http://localhost:8080/swagger-ui.html, OpenAPI JSON at `/v3/api-docs`):
- `GET /api/course/search?keyword=&minPrice=&maxPrice=&sort=&page=&size=` (ADMIN, CUSTOMER): paged course search, sort = `newest` (default) | `priceAsc` | `priceDesc` | `name`, returns `{content, page, size, totalElements, totalPages}`.
- `PUT /api/customer/{customerId}` (CUSTOMER, own profile): body `{customerName, information, mobileNumber?}`, returns the updated customer.
- `GET /api/dashboard/customer/{customerId}` (CUSTOMER, own data): `{enrolledCourses, ordersPlaced, cartItems, totalSpent, reviewsGiven}`.
- `GET /api/dashboard/admin` (ADMIN): `{totalCourses, totalCustomers, totalOrders, totalRevenue, totalReviews, averageRating}`.
- `GET /api/review/course/{courseType}` (ADMIN, CUSTOMER): reviews of one course, `204 No Content` when there are none.
- `GET /api/review/summary` (ADMIN, CUSTOMER): `[{courseType, averageRating, reviewCount}]`.

The login/refresh access token now also carries the claims `userId` and `username` (besides `sub` = email and `role`), so the Angular app can read them with `jwt-decode`.
Guideline checklist mapping: see `docs/GUIDELINES.md`.

## Admin registration code
In production set `ADMIN_INVITE_CODE`; the registration form then asks for it when Admin is chosen. Empty means open (fine for local development).

## Common problems
| Problem | Fix |
|---|---|
| `Access denied for user 'root'` | Wrong MySQL password: set `DB_PASSWORD` |
| `Communications link failure` | MySQL is not running or not on 3306 |
| Port 8080 or 8081 already in use | Stop the other program, or change the port |
| Frontend shows network errors | Backend not running, or `apiUrl` in `angularapp/src/environments/environment.ts` is wrong |
| `npm install` errors | Delete `node_modules` and `package-lock.json`, run `npm install --legacy-peer-deps` |
| Karma: "Could not find Chromium" | Install Chrome and run `export CHROME_BIN=/path/to/chrome` first |
