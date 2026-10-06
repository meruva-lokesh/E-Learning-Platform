import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { InstructorService } from '../../services/instructor.service';
import { Course } from '../../models/course.model';

/**
 * An approved instructor's own courses: list, add, edit and delete. The server decides who owns what, so an instructor
 * can never change another person's course.
 */
@Component({
  selector: 'app-instructor-courses',
  templateUrl: './instructor-courses.component.html',
  styleUrls: ['./instructor-courses.component.css']
})
export class InstructorCoursesComponent implements OnInit {
  courses: Course[] = [];
  loaded = false;
  notApproved = false;
  loadError = '';
  formOpen = false;
  editingId: number | null = null;
  saving = false;
  formError = '';
  message = '';

  form = this.fb.group({
    courseType: ['', [Validators.required, Validators.maxLength(100)]],
    courseImageUrl: ['', [Validators.pattern(/^$|^https?:\/\/.+/), Validators.maxLength(2000)]],
    courseDetails: ['', [Validators.required, Validators.maxLength(2000)]],
    coursePrice: [null as number | null, [Validators.required, Validators.min(0)]]
  });

  constructor(private fb: FormBuilder, private instructors: InstructorService, private dialog: MatDialog) {}

  ngOnInit(): void {
    this.load();
  }

  get f() { return this.form.controls; }

  load(): void {
    this.loadError = '';
    this.instructors.myCourses().subscribe({
      next: list => { this.courses = list; this.loaded = true; },
      error: err => {
        this.loaded = true;
        if (err.status === 403) { this.notApproved = true; } else { this.loadError = 'Could not load your courses. Please try again.'; }
      }
    });
  }

  openAdd(): void {
    this.editingId = null;
    this.form.reset({ courseType: '', courseImageUrl: '', courseDetails: '', coursePrice: null });
    this.formError = '';
    this.message = '';
    this.formOpen = true;
  }

  openEdit(c: Course): void {
    this.editingId = c.courseId ?? null;
    this.form.reset({ courseType: c.courseType, courseImageUrl: c.courseImageUrl || '', courseDetails: c.courseDetails, coursePrice: c.coursePrice ?? null });
    this.formError = '';
    this.message = '';
    this.formOpen = true;
    window.scrollTo({ top: 0 });
  }

  closeForm(): void {
    this.formOpen = false;
    this.editingId = null;
    this.formError = '';
  }

  save(): void {
    if (this.form.invalid || this.saving) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.formError = '';
    const body = this.form.value as Course;
    const call = this.editingId === null ? this.instructors.addCourse(body) : this.instructors.updateCourse(this.editingId, body);
    call.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId === null ? 'Course added.' : 'Course updated.';
        this.closeForm();
        this.load();
      },
      error: err => {
        this.saving = false;
        this.formError = [400, 403, 404].includes(err.status) ? (err.error?.message || 'Please check the entered details')
          : 'Could not save the course. Please try again.';
      }
    });
  }

  askDelete(c: Course): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete course?', message: 'Delete "' + c.courseType + '"? This cannot be undone.', confirmText: 'Yes, delete', cancelText: 'Cancel', icon: 'delete', danger: true }
    }).afterClosed().subscribe(yes => {
      if (!yes || c.courseId === undefined) { return; }
      this.instructors.deleteCourse(c.courseId).subscribe({
        next: () => { this.message = 'Course deleted.'; this.load(); },
        error: err => { this.message = ''; this.loadError = err.error?.message || 'The course could not be deleted. Students may already be enrolled.'; }
      });
    });
  }

  trackById(_: number, c: Course): number | undefined {
    return c.courseId;
  }
}
