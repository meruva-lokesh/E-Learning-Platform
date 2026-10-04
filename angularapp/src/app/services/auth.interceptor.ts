import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { PUBLIC_API_PATTERN } from '../constant';

/**
 * Adds the Bearer token to API calls. When the backend answers 401 (token expired) it
 * refreshes once, retries the call, and only logs the user out if the refresh fails.
 */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService, private router: Router) {}

  private withToken(req: HttpRequest<any>): HttpRequest<any> {
    const token = this.auth.getToken();
    return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isAuthCall = PUBLIC_API_PATTERN.test(req.url);
    const outgoing = isAuthCall ? req : this.withToken(req);

    return next.handle(outgoing).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status !== 401 || isAuthCall) {
          return throwError(() => err);
        }
        if (!this.auth.hasSessionFlag()) {
          this.auth.clearSession();
          this.router.navigate(['/login']);
          return throwError(() => err);
        }
        return this.auth.refresh().pipe(
          catchError((refreshErr) => {
            this.auth.clearSession();
            this.router.navigate(['/login']);
            return throwError(() => refreshErr);
          }),
          switchMap(() => next.handle(this.withToken(req)))
        );
      })
    );
  }
}
