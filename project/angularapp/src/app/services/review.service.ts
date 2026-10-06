import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { API, API_URL, STORAGE } from '../constant';
import { Review } from '../models/review.model';

/**
 * A customer's own reviews: list them and delete one. (Adding and reading reviews stays in CartService.)
 */
@Injectable({
  providedIn: 'root'
})
export class ReviewService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  /** The logged-in customer's reviews. The server answers 404 when there are none, which is shown as an empty list. */
  getMyReviews(): Observable<Review[]> {
    const userId = localStorage.getItem(STORAGE.USER_ID);
    return this.http.get<Review[]>(`${this.apiUrl}${API.REVIEW_BY_USER}/${userId}`).pipe(catchError(() => of([])));
  }

  /** Deletes one of my own reviews (the server refuses somebody else's review with 403). */
  deleteMyReview(reviewId: number): Observable<Review> {
    return this.http.delete<Review>(`${this.apiUrl}${API.REVIEW_MY}/${reviewId}`);
  }
}
