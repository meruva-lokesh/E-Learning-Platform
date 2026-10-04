import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL, STORAGE } from '../constant';

/**
 * Orders (enrollments).
 *
 * @author Tanvi
 */
@Injectable({
  providedIn: 'root'
})
export class OrderService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  addOrder(orderData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.ORDER}`, orderData);
  }

  cancelOrder(orderId: any): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}${API.ORDER}/${orderId}`);
  }

  viewAllOrders(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}${API.ORDER}`);
  }

  viewOrderByCustomerId(): Observable<any> {
    const customerId = localStorage.getItem(STORAGE.CUSTOMER_ID);
    return this.http.get<any>(`${this.apiUrl}${API.ORDER_BY_CUSTOMER}/${customerId}`);
  }

  // the API has no per-user order route, so this uses the customer id of the logged-in user
  viewOrderByUserId(): Observable<any> {
    return this.viewOrderByCustomerId();
  }

  viewOrderById(orderId: any): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}${API.ORDER}/${orderId}`);
  }

  deleteOrder(orderId: any): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}${API.ORDER}/${orderId}`);
  }
}
