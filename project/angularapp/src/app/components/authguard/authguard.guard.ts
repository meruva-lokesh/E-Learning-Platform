import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../../services/auth.service';

/**
 * Blocks pages unless the user is logged in and has one of the roles in the route's data.roles.
 * After a page reload the access token is gone, so the guard first tries to restore the session
 * from the refresh cookie. This is a UX guard only; the backend enforces the same rules.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    const decide = (): boolean | UrlTree => {
      if (!this.auth.isLoggedIn()) {
        return this.router.createUrlTree(['/login']);
      }
      const allowed: string[] | undefined = route.data ? route.data['roles'] : undefined;
      const role = this.auth.getRole();
      if (allowed && (!role || !allowed.includes(role))) {
        // logged in, but this page belongs to the other role: show the error page (the navbar keeps the user's own links)
        return this.router.createUrlTree(['/error'], { queryParams: { code: 403 } });
      }
      return true;
    };

    if (this.auth.isLoggedIn()) {
      return of(decide());
    }
    return this.auth.restoreSession().pipe(map(() => decide()));
  }
}
