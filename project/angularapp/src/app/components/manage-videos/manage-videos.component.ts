import { HttpEventType } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { Course } from '../../models/course.model';
import { CourseVideo, CourseVideoStats, VideoStat, formatDuration, formatSize } from '../../models/video.model';
import { AuthService } from '../../services/auth.service';
import { CourseService } from '../../services/course.service';
import { InstructorService } from '../../services/instructor.service';
import { VideoService } from '../../services/video.service';
import { INSTRUCTOR_ROLE } from '../../models/instructor.model';

/**
 * For the admin and for approved instructors: pick a course, upload videos to it, see them and see how far the
 * customers have watched. An instructor only sees their own courses; the server enforces the same rule.
 */
@Component({
  selector: 'app-manage-videos',
  templateUrl: './manage-videos.component.html',
  styleUrls: ['./manage-videos.component.css']
})
export class ManageVideosComponent implements OnInit, OnDestroy {
  /** Hint for the person uploading; the server has the real limit (video.max-size-mb). */
  readonly maxMb = 200;
  readonly fmtDuration = formatDuration;
  readonly fmtSize = formatSize;

  courses: Course[] = [];
  coursesLoaded = false;
  notApproved = false;
  courseId: number | null = null;

  videos: CourseVideo[] = [];
  stats: CourseVideoStats | null = null;
  videosLoading = false;
  loadError = '';

  title = '';
  description = '';
  file: File | null = null;
  fileDuration = 0;
  fileError = '';
  uploading = false;
  uploadPercent = 0;
  uploadError = '';
  message = '';
  private uploadSub?: Subscription;

  constructor(private auth: AuthService, private courseApi: CourseService, private instructors: InstructorService,
              private videoApi: VideoService, private dialog: MatDialog, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const wanted = Number(this.route.snapshot.queryParamMap.get('courseId'));
    const source = this.auth.getRole() === INSTRUCTOR_ROLE ? this.instructors.myCourses() : this.courseApi.viewAllCourses();
    source.subscribe({
      next: list => {
        this.courses = list;
        this.coursesLoaded = true;
        if (wanted && list.some(c => c.courseId === wanted)) { this.courseId = wanted; this.loadCourse(); }
      },
      error: err => {
        this.coursesLoaded = true;
        if (err.status === 403) { this.notApproved = true; } else { this.loadError = 'Could not load your courses. Please try again.'; }
      }
    });
  }

  ngOnDestroy(): void {
    this.uploadSub?.unsubscribe();
  }

  onCourseChange(): void {
    this.message = '';
    this.uploadError = '';
    this.resetForm();
    this.loadCourse();
  }

  loadCourse(): void {
    if (this.courseId === null) { this.videos = []; this.stats = null; return; }
    const id = this.courseId;
    this.videosLoading = true;
    this.loadError = '';
    forkJoin({
      videos: this.videoApi.list(id),
      stats: this.videoApi.stats(id).pipe(catchError(() => of(null)))
    }).subscribe({
      next: r => {
        if (this.courseId !== id) { return; }
        this.videos = r.videos;
        this.stats = r.stats;
        this.videosLoading = false;
      },
      error: err => {
        this.videosLoading = false;
        this.loadError = err.status === 403 ? 'You cannot manage videos of this course.' : 'Could not load the videos. Please try again.';
      }
    });
  }

  statFor(videoId: number): VideoStat | undefined {
    return this.stats?.videos.find(s => s.videoId === videoId);
  }

  onFileChosen(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.file = null;
    this.fileDuration = 0;
    this.fileError = '';
    const f = input.files && input.files.length ? input.files[0] : null;
    if (!f) { return; }
    if (!/\.(mp4|webm)$/i.test(f.name)) { this.fileError = 'Choose an MP4 or WebM video.'; input.value = ''; return; }
    if (f.size > this.maxMb * 1024 * 1024) { this.fileError = `This video is larger than ${this.maxMb} MB.`; input.value = ''; return; }
    this.file = f;
    if (!this.title.trim()) { this.title = f.name.replace(/\.[^.]+$/, '').replace(/[_-]+/g, ' ').slice(0, 150); }
    this.readDuration(f);
  }

  /** Asks the browser for the length of the chosen file, so the progress percentage can be calculated later. */
  private readDuration(f: File): void {
    const url = URL.createObjectURL(f);
    const probe = document.createElement('video');
    probe.preload = 'metadata';
    const done = () => { URL.revokeObjectURL(url); probe.removeAttribute('src'); };
    probe.onloadedmetadata = () => { this.fileDuration = isFinite(probe.duration) ? Math.round(probe.duration) : 0; done(); };
    probe.onerror = () => { this.fileDuration = 0; done(); };
    probe.src = url;
  }

  get canUpload(): boolean {
    return this.courseId !== null && !!this.file && this.title.trim().length >= 2 && !this.uploading;
  }

  upload(): void {
    if (!this.canUpload || this.courseId === null || !this.file) { return; }
    const form = new FormData();
    form.append('title', this.title.trim());
    if (this.description.trim()) { form.append('description', this.description.trim()); }
    form.append('durationSec', String(this.fileDuration));
    form.append('file', this.file);
    this.uploading = true;
    this.uploadPercent = 0;
    this.uploadError = '';
    this.message = '';
    this.uploadSub = this.videoApi.upload(this.courseId, form).subscribe({
      next: ev => {
        if (ev.type === HttpEventType.UploadProgress && ev.total) {
          this.uploadPercent = Math.min(99, Math.round((100 * ev.loaded) / ev.total));
        } else if (ev.type === HttpEventType.Response) {
          this.uploading = false;
          this.uploadPercent = 100;
          this.message = 'Video uploaded.';
          this.resetForm();
          this.loadCourse();
        }
      },
      error: err => {
        this.uploading = false;
        this.uploadError = err.status === 413 ? (err.error?.message || 'The video is too large.')
          : [400, 403, 404, 429].includes(err.status) ? (err.error?.message || 'The video could not be uploaded.')
          : 'Could not upload the video. Please try again.';
      }
    });
  }

  cancelUpload(): void {
    this.uploadSub?.unsubscribe();
    this.uploading = false;
    this.uploadPercent = 0;
    this.uploadError = 'Upload cancelled.';
  }

  askDelete(v: CourseVideo): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete video?', message: 'Delete "' + v.title + '"? The file and the customers\' watch progress for it are removed.', confirmText: 'Yes, delete', cancelText: 'Cancel', icon: 'delete', danger: true }
    }).afterClosed().subscribe(yes => {
      if (!yes) { return; }
      this.videoApi.delete(v.id).subscribe({
        next: () => { this.message = 'Video deleted.'; this.loadCourse(); },
        error: err => { this.message = ''; this.loadError = err.error?.message || 'The video could not be deleted.'; }
      });
    });
  }

  private resetForm(): void {
    this.title = '';
    this.description = '';
    this.file = null;
    this.fileDuration = 0;
    this.fileError = '';
    const input = document.getElementById('videoFile') as HTMLInputElement | null;
    if (input) { input.value = ''; }
  }

  trackById(_: number, v: CourseVideo): number { return v.id; }
}
