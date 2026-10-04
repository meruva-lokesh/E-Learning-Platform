# Improvements to add next (the existing project stays as it is)

## Listed in the SRS modules but without screens or APIs
1. **Manage user registrations (Admin)**: list all users, disable or delete an account.
2. **Approve feedback (Admin)**: reviews start as "pending"; admin approves before customers see them.
3. **Reports (Admin)**: enrollments per course, revenue per month, average rating per course (charts + CSV download).
4. **Announcements (Admin)**: admin posts a notice; customers see it on their home page.

## Learning experience
5. Lessons and videos inside each course, with "mark as complete" and a progress bar.
6. Certificate (PDF) when a course is completed.
7. Quizzes at the end of each course.
8. Instructor role who manages only their own courses.

## Shopping
9. Real payments (Razorpay or Stripe) instead of the demo payment page.
10. Coupons and wishlist.
11. Email receipt after enrolling.

## Discovery
12. ~~Pagination, sorting, price filter and keyword search~~ (done). Next: categories and levels as filters.
13. Course categories and levels (beginner / advanced) so AI search can use them.

## Accounts and security
14. Forgot password and email verification.
15. ~~Profile page~~ (name, mobile, about: done). Next: change password.
16. Login with Google.
17. Redis for rate limiting when running more than one server.

## Quality and operations
18. Flyway database migrations instead of automatic table updates.
19. ~~Swagger / OpenAPI page for every endpoint~~ (done: `/swagger-ui.html`, springdoc 2.5.0).
20. Integration tests with Testcontainers (real MySQL) and a GitHub Actions pipeline.
21. Docker setup for one-command start (left out on purpose for now).
22. Dark mode, language selection, accessibility audit.
