import { Component, OnInit } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Course } from '../../models/course.model';
import { EarningRow, EarningsSummary } from '../../models/billing.model';
import { InstructorView } from '../../models/instructor.model';
import { EarningsService } from '../../services/earnings.service';
import { InstructorService } from '../../services/instructor.service';

/**
 * The instructor's home page, the first page after login. It only reads data that other pages already use:
 * the application (name, qualification, approval date), the instructor's courses and the earnings summary.
 * Each call has its own fallback, so one failing call leaves the other cards working.
 */
@Component({
  selector: 'app-instructor-home',
  templateUrl: './instructor-home.component.html',
  styleUrls: ['./instructor-home.component.css']
})
export class InstructorHomeComponent implements OnInit {
  loading = true;
  profile: InstructorView | null = null;
  courses: Course[] = [];
  earnings: EarningsSummary | null = null;
  failed = { profile: false, courses: false, earnings: false };

  constructor(private instructors: InstructorService, private earningsApi: EarningsService) { }

  ngOnInit(): void {
    forkJoin({
      profile: this.instructors.me().pipe(catchError(() => {
        this.failed.profile = true; return of(null);
      })),
      courses: this.instructors.myCourses().pipe(catchError(() => {
        this.failed.courses = true; return of([] as Course[]);
      })),
      earnings: this.earningsApi.mine().pipe(catchError(() => {
        this.failed.earnings = true; return of(null);
      }))
    }).subscribe(r => {
      this.profile = r.profile;
      this.courses = r.courses;
      this.earnings = r.earnings;
      this.loading = false;
    });
  }

  /** The name to greet. */
  get name(): string {
    return this.profile?.username || 'instructor';
  }

  get sales(): number { return this.earnings?.earnings?.length || 0; }
  get recentSales(): EarningRow[] { return (this.earnings?.earnings || []).slice(0, 5); }
  get recentCourses(): Course[] { return this.courses.slice(0, 5); }
  get hasCourse(): boolean { return this.courses.length > 0; }
  get hasSale(): boolean { return this.sales > 0; }

  /** How many of the two "getting started" steps are done. */
  get doneSteps(): number { return (this.hasCourse ? 1 : 0) + (this.hasSale ? 1 : 0); }

  /** Money from the server is in paise; show rupees. */
  rupees(paise: number | undefined | null): string {
    return ((paise || 0) / 100).toLocaleString('en-IN', {
      style: 'currency', currency: 'INR',
      maximumFractionDigits: 2
    });
  }

  price(value: number | undefined): string {
    return (value || 0).toLocaleString('en-IN', {
      style: 'currency', currency: 'INR',
      maximumFractionDigits: 0
    });
  }

  trackCourse(_: number, c: Course): number | undefined { return c.courseId; }
}