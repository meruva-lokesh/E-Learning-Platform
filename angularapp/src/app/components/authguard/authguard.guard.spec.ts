import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { AuthGuard } from './authguard.guard';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  const route = (roles?: string[]) => ({ data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot);
  const state = {} as RouterStateSnapshot;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['isLoggedIn', 'getRole', 'restoreSession']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [{ provide: AuthService, useValue: auth }]
    });
    guard = TestBed.inject(AuthGuard);
    router = TestBed.inject(Router);
  });

  it('redirects to /login when there is no session', (done) => {
    auth.isLoggedIn.and.returnValue(false);
    auth.restoreSession.and.returnValue(of(false));
    guard.canActivate(route(['ADMIN']), state).subscribe(result => {
      expect(router.serializeUrl(result as UrlTree)).toBe('/login');
      done();
    });
  });

  it('lets a logged-in user with the right role through', (done) => {
    auth.isLoggedIn.and.returnValue(true);
    auth.getRole.and.returnValue('ADMIN');
    guard.canActivate(route(['ADMIN']), state).subscribe(result => {
      expect(result).toBeTrue();
      done();
    });
  });

  it('shows the error page (not login) when a customer opens an admin page', (done) => {
    auth.isLoggedIn.and.returnValue(true);
    auth.getRole.and.returnValue('CUSTOMER');
    guard.canActivate(route(['ADMIN']), state).subscribe(result => {
      expect(router.serializeUrl(result as UrlTree)).toBe('/error?code=403');
      done();
    });
  });

  it('restores the session after a reload before deciding', (done) => {
    auth.isLoggedIn.and.returnValues(false, true);
    auth.getRole.and.returnValue('CUSTOMER');
    auth.restoreSession.and.returnValue(of(true));
    guard.canActivate(route(['CUSTOMER']), state).subscribe(result => {
      expect(result).toBeTrue();
      done();
    });
  });
});