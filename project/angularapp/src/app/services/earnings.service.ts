import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { EarningsSummary, PayableRow, PayoutView } from '../models/billing.model';

/** Instructor earnings, and the admin's payouts. */
@Injectable({ providedIn: 'root' })
export class EarningsService {
  private apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  mine(): Observable<EarningsSummary> {
    return this.http.get<EarningsSummary>(`${this.apiUrl}${API.INSTRUCTOR_EARNINGS}`);
  }

  payable(): Observable<PayableRow[]> {
    return this.http.get<PayableRow[]>(`${this.apiUrl}${API.ADMIN_PAYOUTS}/payable`);
  }

  history(): Observable<PayoutView[]> {
    return this.http.get<PayoutView[]>(`${this.apiUrl}${API.ADMIN_PAYOUTS}`);
  }

  pay(instructorUserId: number, reference: string): Observable<PayoutView> {
    return this.http.post<PayoutView>(`${this.apiUrl}${API.ADMIN_PAYOUTS}`, { instructorUserId, reference });
  }
}
