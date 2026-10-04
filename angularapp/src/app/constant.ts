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
  REVIEW_SUMMARY: '/api/review/summary'
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
export const PUBLIC_API_PATTERN = /\/api\/(login|register|refresh|logout)$/;

export const PAGE_SIZE = 9;