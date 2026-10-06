/**
 * Every API URL, storage key and role name of the app lives here, so nothing is hard-coded in components or services.
 * The host part (http://localhost:8080) comes from environments/environment.ts.
 *
 * @author Team Lead
 */
import { environment } from '../environments/environment';

export const API_URL: string = environment.apiUrl;

export const API = {
  // authentication (Suriya)
  REGISTER: '/api/register',
  LOGIN: '/api/login',
  REFRESH: '/api/refresh',
  LOGOUT: '/api/logout',
  ADMIN_USERS: '/api/admin/users',

  // courses (Sivamuthu, Amogh)
  COURSE: '/api/course',
  COURSE_SEARCH: '/api/course/search',
  COURSE_AI_SEARCH: '/api/course-ai/search',

  // AI quiz and chatbot
  AI_STATUS: '/api/ai/status',
  AI_QUIZ_GENERATE: '/api/ai/quiz/generate',
  AI_QUIZ_SUBMIT: '/api/ai/quiz/submit',
  AI_QUIZ_HISTORY: '/api/ai/quiz/history',
  AI_CHAT: '/api/ai/chat',

  // mobile OTP (Day 1)
  OTP_STATUS: '/api/otp/status',
  OTP_SEND: '/api/otp/send',
  OTP_VERIFY: '/api/otp/verify',
  // forgot password (direct reset: e-mail, then new password)
  FORGOT_PASSWORD: '/api/forgot-password',
  RESET_PASSWORD: '/api/reset-password',

  // instructors (Day 2)
  INSTRUCTOR_REGISTER: '/api/instructor/register',
  INSTRUCTOR_ME: '/api/instructor/me',
  INSTRUCTOR_COURSES: '/api/instructor/courses',
  VIDEO: '/api/video',
  ADMIN_INSTRUCTORS: '/api/admin/instructors',
  UPLOAD_IMAGE: '/api/upload/image',
  GOOGLE_LOGIN: '/api/auth/google',
  PAYMENT_RAZORPAY_ORDER: '/api/payment/razorpay/order',
  PAYMENT_RAZORPAY_VERIFY: '/api/payment/razorpay/verify',
  INVOICE: '/api/invoice',
  INSTRUCTOR_EARNINGS: '/api/instructor/earnings',
  ADMIN_PAYOUTS: '/api/admin/payouts',
  INSTRUCTOR_STATUS: '/api/instructor/status',
  INSTRUCTOR_RESUBMIT: '/api/instructor/resubmit',

  // customer profile and dashboard (Meruva Lokesh)
  CUSTOMER: '/api/customer',
  CUSTOMER_BY_USER: '/api/customer/user',
  DASHBOARD_CUSTOMER: '/api/dashboard/customer',
  DASHBOARD_ADMIN: '/api/dashboard/admin',

  // cart (Shoryan)
  CART: '/api/cart',
  CART_BY_CUSTOMER: '/api/cart/customer',
  CART_BY_USER: '/api/cart/user',

  // orders (Tanvi)
  ORDER: '/api/order',
  ORDER_BY_CUSTOMER: '/api/order/customer',

  // reviews (Sumit)
  REVIEW: '/api/review',
  REVIEW_BY_USER: '/api/review/user',
  REVIEW_BY_COURSE: '/api/review/course',
  REVIEW_SUMMARY: '/api/review/summary',
  REVIEW_MY: '/api/review/my'
};

/** localStorage keys (the SRS keeps the JWT in localStorage). */
export const STORAGE = {
  TOKEN: 'token',
  ROLE: 'role',
  USER_ID: 'userId',
  CUSTOMER_ID: 'customerId',
  CART_ID: 'cartId',
  SESSION: 'session'
};

export const ROLES = {
  ADMIN: 'ADMIN',
  CUSTOMER: 'CUSTOMER'
};

/** Paths used by the AuthInterceptor to skip the Bearer header. */
export const PUBLIC_API_PATTERN = /\/api\/(login|register|refresh|logout|auth\/google|forgotpassword|reset-password|otp\/(status|send|verify)|instructor\/(register|status|resubmit))$/;


export const PAGE_SIZE = 9;