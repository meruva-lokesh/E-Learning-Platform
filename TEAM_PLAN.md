# Team Plan: 7 Developers

Roles follow the assignment table from the project brief. Each person owns a backend slice and the matching Angular pages.
Branches are the ones already in the repo (`E-Learning-Platform-<name>`).

## Who builds what

| Developer | Feature | Backend (`springapp`) | Frontend (`angularapp/src/app`) |
|---|---|---|---|
| **Suriya** | Authentication and security: login, register, validation, auth guard, role-based navigation, logout | `User`, `AuthController`, `UserService(Impl)`, `UserRepo`, `RefreshToken*`, `JwtUtil`, `JwtRequestFilter`, `SecurityConfig`, rate limiter, login lockout | `modules/auth`, `login`, `registration`, `authguard`, `auth.service`, `auth.interceptor`, `user-store.service` |
| **Sivamuthu** | Course management (admin): add, edit, delete course, details, validation | `Course`, `CourseRepo`, `CourseService(Impl)`, `CourseController`, add / update / delete APIs | `modules/admin`, `add-course`, `edit-course` |
| **Amogh** | Course viewing and search (with AI search): view, search, filter, details page, pagination | `GET /api/course`, `GET /api/course/{id}`, `GET /api/course/search`, `CourseSearchRequest`, `PageResponse` | `customer-view-courses`, `view-courses`, `course-details`, `shared/course-card`, `course.service` |
| **Meruva Lokesh** | Customer profile and dashboard: view / update profile, dashboard UI and statistics | `Customer`, `CustomerController`, `CustomerService(Impl)`, `CustomerRepo`, `DashboardController`, `DashboardService(Impl)`, stats DTOs, `SwaggerConfig` | `dashboard`, `customerdashboard`, `customer-profile`, `customer.service`, `constant.ts`, theme |
| **Shoryan** | Cart: add, view, remove, summary, customer-cart link | `Cart`, `CartRepo`, `CartService(Impl)`, `CartController` | `my-cart`, cart calls in `cart.service` |
| **Tanvi** | Orders and enrollment: enroll, my enrollments, history, details | `Orders`, `OrderRepo`, `OrderService(Impl)`, `OrderController` | `place-order`, `my-orders`, `order-details`, `view-orders`, `order.service` |
| **Sumit** | Reviews and AI search: submit / view reviews, AI search interface, semantic search API | `Review`, `ReviewRepo`, `ReviewService(Impl)`, `ReviewController`, `CourseAiController`, `CourseAiService`, `GeminiService`, `AiUsageLimiter` | `add-review`, `view-review`, `shared/star-rating`, `ai.service` (AI box in the catalog page) |

Shared files (`pom.xml`, `exception/*`, `constant.ts`, `app.module.ts`, `app-routing.module.ts`, `styles.css`, `theme.scss`, `angular.json`): change them only after telling the team in the group chat. They are listed in `.github/CODEOWNERS`.

Rule: edit only files you own. If you need a change in someone else's file, ask the owner.

## Daily routine

| Time | What |
|---|---|
| 9:30 AM | Update your branch from `main` (`git fetch origin`, `git merge origin/main`), run the tests, **open or refresh your Pull Request** |
| 10:00 AM | 10-minute stand-up: done, today, blocked |
| by 12:00 PM | Review the PRs assigned to you |
| by 2:00 PM | Approved PRs are merged by the lead (nobody merges their own PR) |
| 6:30 PM | **Evening push**: commit and push your own branch, even if unfinished (mark the PR as Draft) |

## Git rules
- Work only on your own branch `E-Learning-Platform-<your name>`. Never push straight to `main`.
- Before a PR: `git pull origin main` into your branch, `mvn test` and `npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox` must pass.
- Commit messages: `feat:`, `fix:`, `test:`, `docs:`, `chore:` plus a short sentence.
- Create the PR; do not merge it. 1 approval per PR, 2 for security code (Suriya + one more).

## Before you open a PR (also in `.github/pull_request_template.md`)
- [ ] Builds and tests pass locally
- [ ] No secrets, no commented-out code
- [ ] Endpoint names, ids and attributes still match the SRS
- [ ] New endpoint: role rule, ownership check, Swagger annotations, logging
- [ ] Only files I own were changed (or the owner agreed)
- [ ] Gladiator checklist still holds (`docs/GUIDELINES.md`)

## Suggested order
1. Day 1: everybody runs the app (README) and picks the first task.
2. Week 1: Suriya's auth first (all pages need it); everyone else builds entity, repo, service.
3. Week 2: controllers and pages per module, end to end.
4. Week 3: AI search polish, tests, bug fixing, integration.
5. Last days: demo preparation and README.
