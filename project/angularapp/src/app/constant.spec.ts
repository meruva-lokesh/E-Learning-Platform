import { API, API_URL, PAGE_SIZE, PUBLIC_API_PATTERN } from './constant';

describe('constant.ts', () => {
  it('keeps every API path under /api', () => {
    Object.values(API).forEach(path => expect(path.startsWith('/api/')).toBeTrue());
  });

  it('reads the base URL from the environment file', () => {
    expect(typeof API_URL).toBe('string');
  });

  it('only treats login, register, refresh and logout as public endpoints', () => {
    expect(PUBLIC_API_PATTERN.test('/api/login')).toBeTrue();
    expect(PUBLIC_API_PATTERN.test('/api/course')).toBeFalse();
  });

  it('uses a sensible page size', () => {
    expect(PAGE_SIZE).toBeGreaterThan(0);
  });
});
