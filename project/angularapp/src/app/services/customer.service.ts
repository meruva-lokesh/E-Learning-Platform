import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { API, API_URL, STORAGE } from '../constant';
import { AdminStats, CustomerStats } from '../models/stats.model';

/**
 * Customer profile and dashboard numbers.
 *
 * @author Meruva Lokesh
 */
@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  private remember = (res: any) => {
    if (res && res.customerId) { localStorage.setItem(STORAGE.CUSTOMER_ID, String(res.customerId)); }
  };

  registerCustomer(customer: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.CUSTOMER}`, customer).pipe(tap(this.remember));
  }

  viewCustomerById(customerId: any): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}${API.CUSTOMER}/${customerId}`);
  }

  viewCustomerByUserId(): Observable<any> {
    const userId = localStorage.getItem(STORAGE.USER_ID);
    return this.http.get<any>(`${this.apiUrl}${API.CUSTOMER_BY_USER}/${userId}`).pipe(tap(this.remember));
  }

  /** Saves the customer's name, information and (optionally) mobile number. */
  updateCustomer(customerId: any, details: { customerName: string; information: string; mobileNumber?: string }): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}${API.CUSTOMER}/${customerId}`, details);
  }

  getCustomerStats(customerId: any): Observable<CustomerStats> {
    return this.http.get<CustomerStats>(`${this.apiUrl}${API.DASHBOARD_CUSTOMER}/${customerId}`);
  }

  getAdminStats(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.apiUrl}${API.DASHBOARD_ADMIN}`);
  }
}
