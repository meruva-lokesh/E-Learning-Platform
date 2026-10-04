import { TestBed } from '@angular/core/testing';
import { UserStoreService } from './user-store.service';
import { makeJwt } from '../testing/test-jwt';

describe('UserStoreService', () => {
  let store: UserStoreService;

  beforeEach(() => {
    localStorage.clear();
    store = TestBed.inject(UserStoreService);
  });

  it('decodes the claims of a JWT into the user', () => {
    const user = store.setFromToken(makeJwt({ sub: 'x@y.com', role: 'ADMIN', userId: 9, username: 'nina' }));
    expect(user?.email).toBe('x@y.com');
    expect(user?.role).toBe('ADMIN');
    expect(user?.userId).toBe(9);
    expect(store.user?.username).toBe('nina');
    expect(store.userId).toBe('9');
  });

  it('refuses a token that is not a JWT', () => {
    expect(store.setFromToken('not-a-jwt')).toBeNull();
    expect(store.user).toBeNull();
  });

  it('knows when a token has expired', () => {
    store.setFromToken(makeJwt({ expInSeconds: -60 }));
    expect(store.isTokenValid()).toBeFalse();
    store.setFromToken(makeJwt({ expInSeconds: 600 }));
    expect(store.isTokenValid()).toBeTrue();
  });

  it('restores the user from localStorage and clears it', () => {
    localStorage.setItem('token', makeJwt({ username: 'back' }));
    expect(store.restore()?.username).toBe('back');
    store.clear();
    expect(store.user).toBeNull();
  });
});
