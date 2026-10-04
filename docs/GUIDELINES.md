# Project Gladiator checklist: where each point lives

Numbers match the review sheet. Paths are relative to the project root.
`B` = backend (`springapp/src/main/java/com/examly/springapp`), `F` = frontend (`angularapp/src/app`).

## Backend

| # | Point | Where / how |
|---|---|---|
| 1 | Folder structure and naming | `B/` has `config`, `controller`, `dto`, `exception`, `model`, `repository`, `service` (interface + `…Impl`). Classes are PascalCase, methods camelCase, endpoints lower-case nouns. |
| 2 | DTOs with Spring validation | `dto/CustomerUpdateDTO`, `dto/CourseSearchRequest`, `model/LoginDTO`, and the entities carry `@NotBlank`, `@Size`, `@Pattern`, `@Min`, `@Valid` on controller parameters. |
| 3 | Constructor chaining | Entities have a no-arg, a partial and a full constructor that call each other with `this(...)` (e.g. `model/User`, `model/LoginDTO`). Plain classes are used instead of `@Embedded` because the SRS tables must not change. |
| 4 | Swagger for all endpoints, separate config | `config/SwaggerConfig.java` (title, JWT bearer scheme). Every controller method has `@Operation` and `@ApiResponse`. UI: `http://localhost:8080/swagger-ui.html`. |
| 5 | JWT security | `config/JwtUtil`, `JwtRequestFilter`, `SecurityConfig` (stateless, role rules per endpoint, BCrypt). Token claims: `sub`, `role`, `userId`, `username`. |
| 6 | Structured exception handling, wrapping, funnelling | Hierarchy: `AppException` (abstract, carries the HTTP status) → `ResourceNotFoundException`, `DuplicateResourceException`, `InvalidRequestException`, `RateLimitExceededException`, `ApiCommunicationException`, `DatabaseOperationException`. **Wrapping**: `DatabaseOperationException.guard(...)` wraps raw DB exceptions inside services. **Funnelling**: every error ends in `GlobalExceptionHandler`, which builds one JSON shape and saves it to the `ErrorLog` table. |
| 7 | Status codes | 201 on create, 200 on GET / PUT / DELETE, 204 on `GET /api/review/course/{courseType}` with no reviews, 401 not logged in, 403 wrong role, 409 duplicate, 404 missing, 429 rate limit, 500 unexpected (`GlobalExceptionHandler`). |
| 8 | `ResponseEntity.status().body()` | Used in every controller, e.g. `return ResponseEntity.status(HttpStatus.CREATED).body(saved);`. Alternatives that also work: `ResponseEntity.ok(body)`, `ResponseEntity.created(uri).body(body)`, `ResponseEntity.noContent().build()`, `new ResponseEntity<>(body, status)`, or `@ResponseStatus` on the method. |
| 9 | Logging levels | slf4j `Logger` in controllers and services: `trace` (method entered), `debug` (values and decisions), `info` (business events), `warn`/`error` (problems). Levels are set in `application.properties` (DEBUG for the app, INFO in prod). |
| 10 | Plain getters / setters | No Lombok anywhere (not in `pom.xml` either). |
| 11 | Swagger dependency in pom, one version | `springdoc-openapi-starter-webmvc-ui` with the version held in one property: `<springdoc.version>2.5.0</springdoc.version>`. |
| 12 | Autowire interfaces | Controllers depend on `CourseService`, `CartService`… (interfaces), never on `…Impl`. Constructor injection. |
| 13 | Business logic with author comments | Services contain the rules (ownership checks, duplicate checks, totals, search). Each class has `@author <name>` in its Javadoc, and the key methods have short comments. |
| 14 | Basic validations | Email format, password strength, 10-digit mobile, price > 0, rating 1–5, search page/size limits, identity check (a customer can only read or change their own cart, orders, profile: `service/AccessService`). |

## Frontend

| # | Point | Where / how |
|---|---|---|
| 15 | Folder structure and naming per SRS | `F/components/<name>/<name>.component.{ts,html,css}`, `F/services`, `F/models`, plus `F/modules` (lazy feature modules) and `F/shared`. |
| 16 | User details in a separate service, stored securely | `F/services/user-store.service.ts` holds the authenticated user in memory (`BehaviorSubject`). The token stays in `localStorage` because the SRS demands it; a short-lived access token plus an HttpOnly refresh cookie limits the risk. |
| 17 | Token deconstruction with JWT Decode | `UserStoreService.setFromToken()` uses `jwtDecode` from the `jwt-decode` package to read `sub`, `role`, `userId`, `username`, `exp`. |
| 18 | All API URLs in `constant.ts` | `F/constant.ts` (`API`, `API_URL`, `STORAGE`, `ROLES`). Services never contain a literal URL. |
| 19 | Lazy loading | `F/modules/auth`, `admin`, `customer`, `course-details` are loaded with `loadChildren` in `F/app-routing.module.ts`. |
| 20 | Styling framework, consistent UI | Angular Material with one custom theme (`src/theme.scss`) and shared tokens (`src/styles.css`), Inter + Plus Jakarta Sans fonts. |
| 21 | Form validation with meaningful messages | Reactive forms; every `mat-error` says what to fix ("Enter a valid 10 digit mobile number"). Submit buttons stay disabled until the form is valid. |
| 22 | API URL and keys in environment files | `src/environments/environment.ts` (dev, `http://localhost:8080`) and `environment.prod.ts`, swapped by `fileReplacements` in `angular.json`. |
| 23 | Interfaces for models | `F/models/*.model.ts` (`Course`, `Customer`, `Order`, `Cart`, `Review`, `AuthUser`, `PageResponse<T>`, `CourseRating`, stats…). |
| 24 | Tokens only through an HTTP interceptor | `F/services/auth.interceptor.ts` adds `Authorization: Bearer …` and retries once after a 401 via refresh. No service or component sets the header. |

## Where the SRS and the checklist disagree
- SRS says list endpoints answer 200/404 while the checklist asks for 204: 204 is used only for the new "reviews of a course" endpoint, so SRS tests keep passing.
- SRS says the service sends the token; checklist point 24 says interceptor only. The interceptor is used.
