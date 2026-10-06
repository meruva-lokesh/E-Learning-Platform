import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { OrderService } from './order.service';

/**
 * Which courses has the logged-in customer already bought? Used to show "Go to course" instead of
 * "Add to Cart" so the same course cannot be bought twice (the server refuses it too).
 */
@Injectable({
  providedIn: 'root'
})
export class EnrollmentService {
  constructor(private orders: OrderService) {}

  /** Ids of the bought courses. An empty set when there are no orders (the API answers 404 then) or on any error. */
  enrolledCourseIds(): Observable<Set<number>> {
    return this.orders.viewOrderByCustomerId().pipe(
      map((orders: any[]) => {
        const ids = new Set<number>();
        (orders || []).forEach(o => (o.courses || []).forEach((c: any) => { if (c.courseId != null) { ids.add(c.courseId); } }));
        return ids;
      }),
      catchError(() => of(new Set<number>()))
    );
  }
}
