import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { API, API_URL, STORAGE } from '../constant';
import { CourseRating } from '../models/course-rating.model';
import { Review } from '../models/review.model';

/**
 * Cart calls (Shoryan) and review calls (Sumit), as listed in the SRS.
 *
 * @author Shoryan (cart), Sumit (reviews)
 */
@Injectable({
  providedIn: 'root'
})
export class CartService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  private remember = (cart: any) => {
    if (cart && cart.cartId) { localStorage.setItem(STORAGE.CART_ID, String(cart.cartId)); }
  };

  addToCart(course: any): Observable<any> {
    // the server takes the customer from the login token
    return this.http.post<any>(`${this.apiUrl}${API.CART}`, { courses: [course] }).pipe(tap(this.remember));
  }

  updateCart(cartDetails: any): Observable<any> {
    const cartId = localStorage.getItem(STORAGE.CART_ID);
    return this.http.put<any>(`${this.apiUrl}${API.CART}/${cartId}`, cartDetails);
  }

  removeCoursesFromCart(courseId: any): Observable<any> {
    const cartId = localStorage.getItem(STORAGE.CART_ID);
    return this.http.delete<any>(`${this.apiUrl}${API.CART}/${cartId}/course/${courseId}`);
  }

  removeAllCourses(): Observable<any> {
    const cartId = localStorage.getItem(STORAGE.CART_ID);
    return this.http.delete<any>(`${this.apiUrl}${API.CART}/${cartId}`);
  }

  getCoursesFromCart(cartId: any): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}${API.CART}/${cartId}`);
  }

  getAllCoursesFromCart(): Observable<any> {
    const customerId = localStorage.getItem(STORAGE.CUSTOMER_ID);
    return this.http.get<any>(`${this.apiUrl}${API.CART_BY_CUSTOMER}/${customerId}`).pipe(tap(this.remember));
  }

  addReview(review: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.REVIEW}`, review);
  }

  getAllReviews(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}${API.REVIEW}`);
  }

  /** Reviews of one course. The server answers 204 (empty body) when there are none. */
  getReviewsByCourse(courseType: string): Observable<Review[] | null> {
    return this.http.get<Review[] | null>(`${this.apiUrl}${API.REVIEW_BY_COURSE}/${encodeURIComponent(courseType)}`);
  }

  /** Average rating and review count for every reviewed course. */
  getReviewSummary(): Observable<CourseRating[]> {
    return this.http.get<CourseRating[]>(`${this.apiUrl}${API.REVIEW_SUMMARY}`);
  }
}
