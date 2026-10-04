import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CourseService } from '../../services/course.service';

@Component({
  selector: 'app-edit-course',
  templateUrl: './edit-course.component.html',
  styleUrls: ['./edit-course.component.css']
})
export class EditCourseComponent implements OnInit {
  courseForm: FormGroup;
  courseId = '';
  errorMessage = '';
  loading = false;
  imgFailed = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private snack: MatSnackBar
  ) {
    this.courseForm = this.fb.group({
      courseType: ['', [Validators.required]],
      courseImageUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
      courseDetails: ['', [Validators.required]],
      coursePrice: [null, [Validators.required, Validators.min(0)]]
    });
  }

  get f() { return this.courseForm.controls; }

  ngOnInit(): void {
    this.courseId = this.route.snapshot.paramMap.get('id') || '';
    this.courseService.getCourseById(this.courseId).subscribe({
      next: (course) => this.courseForm.patchValue(course),
      error: () => (this.errorMessage = 'Could not load this course.')
    });
  }

  onSubmit(): void {
    if (this.courseForm.invalid || this.loading) { return; }
    this.loading = true;
    this.courseService.updateCourse(this.courseId, this.courseForm.value).subscribe({
      next: () => { this.loading = false; this.snack.open('Course updated', 'OK', { duration: 3000 }); this.router.navigate(['/view-courses']); },
      error: () => { this.loading = false; this.errorMessage = 'Could not update the course. Please try again.'; }
    });
  }

  cancel(): void {
    this.router.navigate(['/view-courses']);
  }
}