import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';

/** Answer of step 1. requireMobile = step 2 must also ask for the registered mobile number. */
export interface ForgotStartResponse {
  requireMobile: boolean;
}

/** Forgot password without e-mail: step 1 checks the e-mail, step 2 sets the new password. Both calls are public. */
@Injectable({
  providedIn: 'root'
})
export class PasswordResetService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  start(email: string): Observable<ForgotStartResponse> {
    return this.http.post<ForgotStartResponse>(`${this.apiUrl}${API.FORGOT_PASSWORD}`, { email });
  }

  reset(email: string, newPassword: string, mobile?: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}${API.RESET_PASSWORD}`, { email, newPassword, mobile: mobile || null });
  }
}
