import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { PAGE_SIZE } from '../../constant';
import { Course } from '../../models/course.model';
import { CourseRating } from '../../models/course-rating.model';
import { CartService } from '../../services/cart.service';
import { CourseService } from '../../services/course.service';

/**
 * Admin course list with search and paging; Edit and Delete per course.
 *
 * @author Sivamuthu (edit, delete), Amogh (search, pagination)
 */
@Component({
  selector: 'app-view-courses',
  templateUrl: './view-courses.component.html',
  styleUrls: ['./view-courses.component.css']
})
export class ViewCoursesComponent implements OnInit {
  courses: Course[] = [];
  ratings: { [courseType: string]: CourseRating } = {};
  keyword = '';
  page = 0;
  pageSize = PAGE_SIZE;
  totalElements = 0;
  errorMessage = '';
  loaded = false;

  constructor(
    private courseService: CourseService,
    private cartService: CartService,
    private router: Router,
    private dialog: MatDialog,
    private snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cartService.getReviewSummary().subscribe({
      next: (list) => (list || []).forEach(r => (this.ratings[r.courseType || ''] = r)),
      error: () => (this.ratings = {})
    });
    this.load();
  }

  load(): void {
    this.errorMessage = '';
    this.courseService.searchCourses({ keyword: this.keyword.trim(), page: this.page, size: this.pageSize, sort: 'newest' }).subscribe({
      next: (res) => { this.courses = res.content || []; this.totalElements = res.totalElements || 0; this.loaded = true; },
      error: () => { this.errorMessage = 'Could not load courses.'; this.loaded = true; }
    });
  }

  search(): void {
    this.page = 0;
    this.load();
  }

  onPage(event: PageEvent): void {
    this.page = event.pageIndex;
    this.load();
  }

  ratingFor(course: Course): CourseRating | undefined {
    return this.ratings[course.courseType || ''];
  }

  edit(course: Course): void {
    this.router.navigate(['/edit-course', course.courseId]);
  }

  delete(course: Course): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete course?',
        message: `Are you sure you want to delete the course "${course.courseType}"? This cannot be undone.`,
        confirmText: 'Delete',
        cancelText: 'Cancel',
        icon: 'warning',
        danger: true
      }
    }).afterClosed().subscribe(yes => {
      if (!yes) { return; }
      this.courseService.deleteCourse(String(course.courseId)).subscribe({
        next: () => {
          this.snack.open('Course deleted', 'OK', { duration: 3000 });
          if (this.courses.length === 1 && this.page > 0) { this.page--; }
          this.load();
        },
        error: () => (this.errorMessage = 'Could not delete the course.')
      });
    });
  }
}
