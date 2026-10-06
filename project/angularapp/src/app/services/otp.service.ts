import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';

export interface OtpSendResponse {
  required: boolean;
  message: string;
  resendInSeconds: number;
  devCode?: string | null;
}

export interface OtpVerifyResponse {
  verified: boolean;
  message: string;
}

/** Mobile number verification by OTP (public endpoints: the user cannot log in until the number is verified). */
@Injectable({
  providedIn: 'root'
})
export class OtpService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  status(): Observable<{ required: boolean }> {
    return this.http.get<{ required: boolean }>(`${this.apiUrl}${API.OTP_STATUS}`);
  }

  send(email: string): Observable<OtpSendResponse> {
    return this.http.post<OtpSendResponse>(`${this.apiUrl}${API.OTP_SEND}`, { email });
  }

  verify(email: string, code: string): Observable<OtpVerifyResponse> {
    return this.http.post<OtpVerifyResponse>(`${this.apiUrl}${API.OTP_VERIFY}`, { email, code });
  }
}
