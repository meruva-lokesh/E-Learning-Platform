/** Builds an unsigned JWT-shaped string for specs (the app only decodes tokens, the server verifies them). */
export function makeJwt(claims: { sub?: string; role?: string; userId?: number; username?: string; expInSeconds?: number } = {}): string {
  const b64 = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  const exp = Math.floor(Date.now() / 1000) + (claims.expInSeconds ?? 900);
  return `${b64({ alg: 'HS256', typ: 'JWT' })}.${b64({ sub: claims.sub ?? 'a@b.com', role: claims.role ?? 'CUSTOMER', userId: claims.userId ?? 1, username: claims.username ?? 'tester', exp })}.sig`;
}
