import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Course } from '../../models/course.model';
import { Review } from '../../models/review.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { CourseService } from '../../services/course.service';
import { ROLES } from '../../constant';

/**
 * Course details page: summary band, price card with Add to Cart / Enroll, and the reviews of the course.
 *
 * @author Amogh (course details), Sumit (reviews)
 */
@Component({
  selector: 'app-course-details',
  templateUrl: './course-details.component.html',
  styleUrls: ['./course-details.component.css']
})
export class CourseDetailsComponent implements OnInit {
  course?: Course;
  reviews: Review[] = [];
  average = 0;
  errorMessage = '';
  loaded = false;
  role: string | null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private cartService: CartService,
    private auth: AuthService,
    private snack: MatSnackBar
  ) {
    this.role = this.auth.getRole();
  }

  get isCustomer(): boolean {
    return this.role === ROLES.CUSTOMER;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.courseService.getCourseById(id).subscribe({
      next: (course) => {
        this.course = course;
        this.loaded = true;
        this.loadReviews(course.courseType || '');
      },
      error: () => { this.errorMessage = 'This course could not be found.'; this.loaded = true; }
    });
  }

  private loadReviews(courseType: string): void {
    this.cartService.getReviewsByCourse(courseType).subscribe({
      // 204 No Content arrives as a null body
      next: (data) => {
        this.reviews = data || [];
        const total = this.reviews.reduce((sum, r) => sum + (r.rating || 0), 0);
        this.average = this.reviews.length ? total / this.reviews.length : 0;
      },
      error: () => (this.reviews = [])
    });
  }

  addToCart(enrollNow = false): void {
    if (!this.course) { return; }
    this.cartService.addToCart(this.course).subscribe({
      next: () => {
        if (enrollNow) { this.router.navigate(['/my-cart']); return; }
        this.snack.open(`${this.course?.courseType} added to your cart`, 'OK', { duration: 2500 });
      },
      error: () => (this.errorMessage = 'Could not add the course to your cart.')
    });
  }

  back(): void {
    this.router.navigate([this.role === ROLES.ADMIN ? '/view-courses' : '/customer-view-courses']);
  }
}
