# START HERE: everything added since day one, merged into your repo

This folder is your GitHub project (commit bfe6290, 4 Oct) with **every change from our chats merged in**.
`CHANGED-FILES.txt` (one level up, inside the zip) lists each file as NEW or CHANGED compared with your repo.
`docs/claude-guides/` has the PDFs that explain each change (backend and frontend separately).

## Fastest way
1. Back up your own folder, then `git pull` so you have your teammates' latest work.
2. Copy `springapp/` and `angularapp/` from this zip over yours. Only copy files listed in CHANGED-FILES.txt if teammates changed the same files; compare those first.
3. In `angularapp`: `npm install` then `ng serve --port 8081`.
4. In `springapp`: set the environment variables below, then `mvn spring-boot:run`. Tables are created automatically (`ddl-auto=update`).
5. If the repo's old folder `angularapp/src/app/manage-admins/` still exists, delete it. The page now lives in `components/manage-admins/`.

## What is inside (by feature)
| Feature | Backend | Frontend | Guide (PDF) |
|---|---|---|---|
| Uploads, footer, registration rules, delete my review, no second purchase | UploadController, UploadConfig, OrderServiceImpl, ReviewController | registration, add/edit course, footer, cart, reviews | CHANGES-7 |
| Midnight dark UI | none | styles.css, theme.scss, all component css | MIDNIGHT-CSS-FINAL |
| AI quiz, chatbot | AiAssistantController, QuizService, ChatService, QuizSessionStore, AiFeatureLimiter, QuizAttempt (**added now; they were missing from the first zip**) | ai-quiz, ai-chatbot | AI-BACKEND/FRONTEND-GUIDE |
| **Forgot password (direct: e-mail, then new password, saved at once; no e-mail sent)** | PasswordResetService, PasswordResetController, ResetDtos | login link, one two-step forgot-password page | FORGOT-PASSWORD-BACKEND/FRONTEND |
| Mobile OTP | sms/*, OtpService, OtpController, PhoneVerification, AuthController edits | verify-phone, otp.service, OTP steps inside both registration pages | DAY1-OTP-*, OTP-DAY1-BACKEND-STEP-BY-STEP |
| Instructors | InstructorService, controllers, models | instructor-register (with OTP), admin-instructors, instructor-courses | DAY2-INSTRUCTOR-* |
| Google OAuth (SME asked for OAuth): **Sign up with Google** on the Registration page stores the account; **Sign in with Google** on the Login page works only for registered e-mails (else "No account found ... register first") | GoogleAuthService/Controller (field `mode`: login or register), pom.xml dependency | login + registration pages, google-auth.service | DAY2-GOOGLE-OAUTH-*, OAUTH-GOOGLE-GUIDE |
| Videos | video/*, VideoService, VideoController | manage-videos, watch-video, my-learning | DAY3-VIDEOS-* |
| Razorpay test payments | Payment*, Razorpay*, PaymentController | place-order, payment.service | PAY-DAY1-*, RAZORPAY-TEST-GUIDE |
| Owner earnings and payouts | EarningService, EarningsController, CourseEarning, Payout | instructor-earnings, admin-payouts | PAY-DAY2-* |
| Invoices with PDF | InvoiceService, InvoicePdf, InvoiceController | my-invoices, invoice-view | PAY-DAY3-* |

Existing backend files I edited: `SecurityConfig` (final version, all rules), `AuthController` (OTP hooks), `RateLimitFilter` (video stream, Google, forgot password), `application.properties` (new lines at the end; the two multipart size lines changed), `pom.xml` (one dependency: spring-security-oauth2-jose), `OrderServiceImpl`, `ReviewController`, `.gitignore`.

## Settings you must fill (nothing secret is in the zip)
Set these as environment variables, or edit the defaults in `application.properties`:
```
OTP_REQUIRED=true
OTP_PROVIDER=dev            # twilio for real SMS (spell it exactly: twilio)
OTP_DEV_EXPOSE_CODE=true    # false with twilio
TWILIO_ACCOUNT_SID=  TWILIO_AUTH_TOKEN=  TWILIO_FROM_NUMBER=   # the Twilio number, not your phone
GOOGLE_CLIENT_ID=           # from Google Cloud Console; also put it in the Angular environment file
RAZORPAY_KEY_ID=  RAZORPAY_KEY_SECRET=   # test keys
RESET_REQUIRE_MOBILE=false  # forgot password: true = also ask for the account's registered mobile number (safer)
COMMISSION_PERCENT=20       # the platform's share; the instructor gets the rest
INVOICE_GST_PERCENT=0       # your company's decision
```
**Security:** your repo's `application.properties` has a real database password as the default (`DB_PASSWORD`). Remove it from the file and from git history; use the environment variable.

## Test order (about 20 minutes)
1. Register a customer with `OTP_REQUIRED=true`: step 2 appears, enter the code, then log in.
2. Register an instructor the same way; log in as admin and approve; the instructor adds a course.
3. Admin uploads a video; a customer who bought the course watches it.
4. Google OAuth: on Registration click Sign up with Google (row appears in `users`); on Login use a Gmail that never registered (must show the error and add no row).
5. Login page: Forgot password? Type the e-mail, Continue, type a new password, Change password, log in with it. (Admin accounts are refused on purpose.)
6. Pay with a Razorpay test card; check the invoice, the instructor's earnings, and record a payout as admin.

## Honest status (please read)
- **The Spring Boot backend has never been compiled or run by me against MySQL.** I tested the Java logic with my own stand-in classes (OTP, instructor, earnings, invoice checks) and every import resolves inside the merged tree, but run `mvn spring-boot:run` once and fix any small compile message before the demo. Send me the first error if you get one.
- The Angular code was built and tested in a real browser against a mock backend (customer and instructor OTP steps, instructor flow, videos, billing).
- Twilio, Google and Razorpay were never called for real.
- Forgot password (direct reset): the Java logic passed 56 checks with stand-in classes and the page passed 29 browser checks against a mock backend. **Security:** with no e-mail or SMS check, anyone who knows an account's e-mail can change its password (admins are blocked; attempts are rate limited). Set `RESET_REQUIRE_MOBILE=true` before showing it to outsiders. The e-mailed link version was removed at your request.
- **Keycloak was NOT built.** The SME suggested OAuth/Keycloak; what is here is Google OAuth login on top of your existing JWT.
- Not built: Razorpay Route (automatic split), refunds, webhooks, e-mailed invoices.
- The PDF invoice prints "Rs" and shows "?" for non-Latin text.
