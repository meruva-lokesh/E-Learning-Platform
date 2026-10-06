/** The logged-in user, read from the decoded JWT. */
export interface AuthUser {
  userId?: number;
  username?: string;
  email?: string;
  role?: string;
  exp?: number;
}
