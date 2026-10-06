import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CourseService } from '../../services/course.service';
import { ImageUploadService } from '../../services/image-upload.service';

@Component({
  selector: 'app-edit-course',
  templateUrl: './edit-course.component.html',
  styleUrls: ['./edit-course.component.css']
})
export class EditCourseComponent implements OnInit, OnDestroy {
  courseForm: FormGroup;
  courseId = '';
  errorMessage = '';
  loading = false;
  imgFailed = false;
  uploading = false;
  uploadError = '';
  previewUrl = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private snack: MatSnackBar,
    private images: ImageUploadService
  ) {
    this.courseForm = this.fb.group({
      courseType: ['', [Validators.required]],
      courseImageUrl: ['', [Validators.required, Validators.pattern(/^https?:\/\/.+/)]],
      courseDetails: ['', [Validators.required]],
      coursePrice: [null, [Validators.required, Validators.min(0)]]
    });
  }

  get f() { return this.courseForm.controls; }

  /** The admin picked a file: check it, show a preview at once, upload it, and keep the returned URL in the form. */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';   // lets the same file be chosen again
    if (!file) { return; }
    this.uploadError = '';
    const problem = this.images.validate(file);
    if (problem) { this.uploadError = problem; return; }
    this.clearPreview();
    this.previewUrl = URL.createObjectURL(file);
    this.imgFailed = false;
    this.uploading = true;
    this.f['courseImageUrl'].markAsTouched();
    this.images.upload(file).subscribe({
      next: (url) => { this.f['courseImageUrl'].setValue(url); this.uploading = false; },
      error: (err) => {
        this.uploading = false;
        this.clearPreview();
        this.uploadError = err.error?.message || 'Could not upload the image. Please try again.';
      }
    });
  }

  private clearPreview(): void {
    if (this.previewUrl) { URL.revokeObjectURL(this.previewUrl); }
    this.previewUrl = '';
  }

  ngOnDestroy(): void {
    this.clearPreview();
  }

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