import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { makeJwt } from '../testing/test-jwt';

@Component({ template: '' })
class BlankComponent {}

const OLD = makeJwt({ username: 'old' });
const NEW = makeJwt({ username: 'new' });

describe('AuthInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      declarations: [BlankComponent],
      imports: [HttpClientTestingModule, RouterTestingModule.withRoutes([{ path: 'login', component: BlankComponent }])],
      providers: [{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }]
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    auth.login({ email: 'a@b.com', password: 'x' }).subscribe();
    ctrl.expectOne(r => r.url.endsWith('/api/login')).flush({ token: OLD, role: 'CUSTOMER', userId: 1 });
  });

  afterEach(() => ctrl.verify());

  it('adds the bearer token to API calls', () => {
    http.get('/api/course').subscribe();
    const req = ctrl.expectOne('/api/course');
    expect(req.request.headers.get('Authorization')).toBe('Bearer ' + OLD);
    req.flush([]);
  });

  it('refreshes once on 401 and retries with the new token', (done) => {
    http.get('/api/course').subscribe(res => {
      expect(res).toEqual(['ok']);
      done();
    });
    ctrl.expectOne('/api/course').flush({}, { status: 401, statusText: 'Unauthorized' });
    ctrl.expectOne(r => r.url.endsWith('/api/refresh')).flush({ token: NEW, role: 'CUSTOMER', userId: 1 });
    const retry = ctrl.expectOne('/api/course');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer ' + NEW);
    retry.flush(['ok']);
  });

  it('logs the user out when the refresh fails', (done) => {
    http.get('/api/course').subscribe({
      error: () => {
        expect(auth.isLoggedIn()).toBeFalse();
        done();
      }
    });
    ctrl.expectOne('/api/course').flush({}, { status: 401, statusText: 'Unauthorized' });
    ctrl.expectOne(r => r.url.endsWith('/api/refresh')).flush({}, { status: 401, statusText: 'Unauthorized' });
  });
});
