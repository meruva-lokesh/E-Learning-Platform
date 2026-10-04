import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { UserStoreService } from './user-store.service';
import { makeJwt } from '../testing/test-jwt';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('stores the JWT, role and user id in localStorage after login (as the SRS requires)', () => {
    service.login({ email: 'a@b.com', password: 'x' }).subscribe();
    const req = http.expectOne(r => r.url.endsWith('/api/login'));
    expect(req.request.withCredentials).toBeTrue();
    const jwt = makeJwt({ role: 'ADMIN', userId: 7, username: 'boss' });
    req.flush({ token: jwt, role: 'ADMIN', userId: 7 });

    expect(service.isLoggedIn()).toBeTrue();
    expect(service.getToken()).toBe(jwt);
    expect(TestBed.inject(UserStoreService).user?.username).toBe('boss');
    expect(service.getRole()).toBe('ADMIN');
    expect(localStorage.getItem('token')).toBe(jwt);
    expect(localStorage.getItem('role')).toBe('ADMIN');
    expect(localStorage.getItem('userId')).toBe('7');
  });

  it('clears everything on logout and tells the server', () => {
    service.login({ email: 'a@b.com', password: 'x' }).subscribe();
    http.expectOne(r => r.url.endsWith('/api/login')).flush({ token: makeJwt(), role: 'CUSTOMER', userId: 1 });
    service.logout();
    http.expectOne(r => r.url.endsWith('/api/logout')).flush(null);

    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getRole()).toBeNull();
    expect(localStorage.getItem('session')).toBeNull();
  });

  it('does not call the server to restore a session when there was never a login', (done) => {
    service.restoreSession().subscribe(ok => {
      expect(ok).toBeFalse();
      done();
    });
    http.expectNone(r => r.url.endsWith('/api/refresh'));
  });

  it('restores the session from the refresh cookie when the flag is set', (done) => {
    localStorage.setItem('session', '1');
    const fresh = makeJwt({ role: 'ADMIN', userId: 2 });
    service.restoreSession().subscribe(ok => {
      expect(ok).toBeTrue();
      expect(service.getToken()).toBe(fresh);
      done();
    });
    http.expectOne(r => r.url.endsWith('/api/refresh')).flush({ token: fresh, role: 'ADMIN', userId: 2 });
  });

  it('shares one refresh request between concurrent callers', () => {
    localStorage.setItem('session', '1');
    service.refresh().subscribe();
    service.refresh().subscribe();
    const reqs = http.match(r => r.url.endsWith('/api/refresh'));
    expect(reqs.length).toBe(1);
    reqs[0].flush({ token: makeJwt({ role: 'ADMIN' }), role: 'ADMIN', userId: 1 });
  });
});
