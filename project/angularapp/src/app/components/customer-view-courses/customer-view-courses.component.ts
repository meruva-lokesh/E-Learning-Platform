import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { PAGE_SIZE } from '../../constant';
import { Course } from '../../models/course.model';
import { CourseRating } from '../../models/course-rating.model';
import { CourseSearchParams } from '../../models/search-params.model';
import { AiService } from '../../services/ai.service';
import { CartService } from '../../services/cart.service';
import { CourseService } from '../../services/course.service';
import { EnrollmentService } from '../../services/enrollment.service';

/**
 * Course catalog for customers: keyword search, price filter, sorting, server-side paging,
 * and the AI (semantic) search box. Ratings come from the review summary API.
 *
 * @author Amogh (catalog, filters, pagination), Sumit (AI search)
 */
@Component({
  selector: 'app-customer-view-courses',
  templateUrl: './customer-view-courses.component.html',
  styleUrls: ['./customer-view-courses.component.css']
})
export class CustomerViewCoursesComponent implements OnInit, OnDestroy {
  courses: Course[] = [];
  ratings: { [courseType: string]: CourseRating } = {};
  inCart = new Set<number>();
  enrolled = new Set<number>();   // courses this customer already bought
  errorMessage = '';
  toastMessage = '';
  loaded = false;

  // filters (server-side)
  filters: CourseSearchParams = { keyword: '', minPrice: null, maxPrice: null, sort: 'newest', page: 0, size: PAGE_SIZE };
  totalElements = 0;
  pageSize = PAGE_SIZE;
  priceError = '';

  // AI search state
  searchText = '';          // the one search box: used by Search (keyword) and AI Search
  aiActive = false;
  aiLoading = false;
  aiError = '';
  activeQuery = '';

  private sub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private courseService: CourseService,
    private cartService: CartService,
    private aiService: AiService,
    private snack: MatSnackBar,
    private enrollment: EnrollmentService
  ) {}

  ngOnInit(): void {
    this.enrollment.enrolledCourseIds().subscribe(ids => (this.enrolled = ids));
    this.cartService.getReviewSummary().subscribe({
      next: (list) => (list || []).forEach(r => (this.ratings[r.courseType || ''] = r)),
      error: () => (this.ratings = {})
    });
    // the navbar search box puts its text in ?keyword=
    this.sub = this.route.queryParamMap.subscribe(params => {
      this.filters.keyword = params.get('keyword') || '';
      this.searchText = this.filters.keyword;
      this.filters.page = 0;
      this.aiActive = false;
      this.load();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get hasFilters(): boolean {
    return !!(this.filters.keyword || this.filters.minPrice != null || this.filters.maxPrice != null);
  }

  load(): void {
    this.errorMessage = '';
    this.courseService.searchCourses(this.filters).subscribe({
      next: (res) => {
        this.courses = res.content || [];
        this.totalElements = res.totalElements || 0;
        this.loaded = true;
      },
      error: () => { this.errorMessage = 'Could not load courses.'; this.loaded = true; }
    });
  }

  /** Plain keyword search (the Search button or Enter in the search box). */
  keywordSearch(): void {
    this.filters.keyword = this.searchText.trim();
    this.applyFilters();
  }

  applyFilters(): void {
    const { minPrice, maxPrice } = this.filters;
    this.priceError = minPrice != null && maxPrice != null && minPrice > maxPrice ? 'Minimum price cannot be more than maximum price' : '';
    if (this.priceError) { return; }
    this.filters.page = 0;
    this.aiActive = false;
    this.load();
  }

  clearFilters(): void {
    this.filters = { keyword: '', minPrice: null, maxPrice: null, sort: 'newest', page: 0, size: PAGE_SIZE };
    this.searchText = '';
    this.aiActive = false;
    this.priceError = '';
    this.load();
  }

  onPage(event: PageEvent): void {
    this.filters.page = event.pageIndex;
    this.load();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  ratingFor(course: Course): CourseRating | undefined {
    return this.ratings[course.courseType || ''];
  }

  aiSearch(): void {
    const query = this.searchText.trim();
    if (!query || this.aiLoading) { return; }
    this.aiLoading = true;
    this.aiError = '';
    this.aiService.searchCourses(query).subscribe({
      next: (results) => {
        this.courses = results;
        this.aiActive = true;
        this.activeQuery = query;
        this.aiLoading = false;
        this.loaded = true;
      },
      error: (err) => {
        this.aiLoading = false;
        this.aiError = err.status === 429
          ? (err.error?.message || 'You have reached the AI search limit. Please try again later.')
          : 'AI search failed. Please try again.';
      }
    });
  }

  clearAiSearch(): void {
    this.searchText = this.filters.keyword || '';
    this.activeQuery = '';
    this.aiActive = false;
    this.aiError = '';
    this.load();
  }

  addToCart(course: Course): void {
    if (course.courseId != null && this.enrolled.has(course.courseId)) {
      this.snack.open('You are already enrolled in this course', 'OK', { duration: 2500 });
      return;
    }
    this.cartService.addToCart(course).subscribe({
      next: () => {
        if (course.courseId != null) { this.inCart.add(course.courseId); }
        this.toastMessage = `${course.courseType} added to your cart`;
        this.snack.open(this.toastMessage, 'OK', { duration: 2500 });
      },
      error: () => {
        this.toastMessage = '';
        this.errorMessage = 'Could not add the course to your cart.';
      }
    });
  }
}