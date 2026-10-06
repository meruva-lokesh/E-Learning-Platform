import { Injectable } from '@angular/core';
import { jwtDecode } from 'jwt-decode';
import { BehaviorSubject } from 'rxjs';
import { STORAGE } from '../constant';
import { AuthUser } from '../models/auth-user.model';

/**
 * Keeps the authenticated user in one place (guideline 16) and builds it by decoding the JWT (guideline 17).
 * The user object lives in memory only; the token itself stays in localStorage because the SRS asks for that.
 * Components never read the token directly: they ask this service who is logged in.
 *
 * @author Suriya
 */
@Injectable({
  providedIn: 'root'
})
export class UserStoreService {
  private userSubject = new BehaviorSubject<AuthUser | null>(null);
  /** Emits the current user, or null when nobody is logged in. */
  public user$ = this.userSubject.asObservable();

  /** Decodes a token and remembers the user. Returns null (and clears the user) when the token is not a valid JWT. */
  setFromToken(token: string | null): AuthUser | null {
    if (!token) {
      this.userSubject.next(null);
      return null;
    }
    try {
      const c: any = jwtDecode(token);
      const user: AuthUser = {
        userId: c.userId !== undefined ? Number(c.userId) : undefined,
        username: c.username,
        email: c.sub,
        role: c.role,
        exp: c.exp
      };
      this.userSubject.next(user);
      return user;
    } catch {
      this.userSubject.next(null);
      return null;
    }
  }

  /** Sets the user directly (used when the token cannot be decoded). */
  setUser(user: AuthUser): void {
    this.userSubject.next(user);
  }

  /** Restores the user after a page reload from the token in localStorage. */
  restore(): AuthUser | null {
    return this.setFromToken(localStorage.getItem(STORAGE.TOKEN));
  }

  clear(): void {
    this.userSubject.next(null);
  }

  get user(): AuthUser | null {
    return this.userSubject.value;
  }

  get role(): string | null {
    return this.userSubject.value?.role ?? null;
  }

  get userId(): string | null {
    const id = this.userSubject.value?.userId;
    return id !== undefined && id !== null ? String(id) : null;
  }

  /** True when the token has not expired yet. */
  isTokenValid(): boolean {
    const exp = this.userSubject.value?.exp;
    return !exp || exp * 1000 > Date.now();
  }
}
