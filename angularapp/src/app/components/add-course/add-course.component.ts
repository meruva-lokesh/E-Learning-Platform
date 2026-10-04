import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CourseService } from '../../services/course.service';

@Component({
  selector: 'app-add-course',
  templateUrl: './add-course.component.html',
  styleUrls: ['./add-course.component.css']
})
export class AddCourseComponent {
  courseForm: FormGroup;
  errorMessage = '';
  loading = false;
  imgFailed = false;

  constructor(private fb: FormBuilder, private courseService: CourseService, private router: Router, private snack: MatSnackBar) {
    this.courseForm = this.fb.group({
      courseType: ['', [Validators.required]],
      courseImageUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
      courseDetails: ['', [Validators.required]],
      coursePrice: [null, [Validators.required, Validators.min(0)]]
    });
  }

  get f() { return this.courseForm.controls; }

  onSubmit(): void {
    if (this.courseForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    this.courseService.addCourse(this.courseForm.value).subscribe({
      next: () => { this.loading = false; this.snack.open('Course added', 'OK', { duration: 3000 }); this.router.navigate(['/view-courses']); },
      error: () => { this.loading = false; this.errorMessage = 'Could not add the course. Please try again.'; }
    });
  }
}