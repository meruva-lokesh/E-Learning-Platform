import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CourseService } from '../../services/course.service';
import { ImageUploadService } from '../../services/image-upload.service';

@Component({
  selector: 'app-add-course',
  templateUrl: './add-course.component.html',
  styleUrls: ['./add-course.component.css']
})
export class AddCourseComponent implements OnDestroy {
  courseForm: FormGroup;
  errorMessage = '';
  loading = false;
  imgFailed = false;
  uploading = false;
  uploadError = '';
  previewUrl = '';

  constructor(private fb: FormBuilder, private courseService: CourseService, private router: Router, private snack: MatSnackBar, private images: ImageUploadService) {
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