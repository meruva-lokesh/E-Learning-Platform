import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { VideoService } from '../../services/video.service';
import { CourseVideo, formatDuration } from '../../models/video.model';
import { ROLES } from '../../constant';

/**
 * The lesson player. A customer's position is saved every few seconds, when they pause, switch lesson or leave,
 * so the next visit continues where they stopped. Admins and instructors can watch too, but have no progress.
 */
@Component({
  selector: 'app-watch-video',
  templateUrl: './watch-video.component.html',
  styleUrls: ['./watch-video.component.css']
})
export class WatchVideoComponent implements OnInit, OnDestroy {
  readonly fmtDuration = formatDuration;
  /** How often the position is saved while the video plays, in milliseconds. */
  private readonly saveEveryMs = 10000;

  courseId = 0;
  videos: CourseVideo[] = [];
  current: CourseVideo | null = null;
  src = '';
  loading = true;
  error = '';
  playError = '';
  resumedAt = 0;
  isCustomer = false;

  private player?: ElementRef<HTMLVideoElement>;
  private timer: any = null;
  private startAt = 0;
  private retried = false;

  @ViewChild('player') set playerRef(ref: ElementRef<HTMLVideoElement> | undefined) { this.player = ref; }

  constructor(private route: ActivatedRoute, private router: Router, private api: VideoService, private auth: AuthService) {}

  ngOnInit(): void {
    this.isCustomer = this.auth.getRole() === ROLES.CUSTOMER;
    this.courseId = Number(this.route.snapshot.paramMap.get('courseId'));
    const wanted = Number(this.route.snapshot.queryParamMap.get('video'));
    this.api.list(this.courseId).subscribe({
      next: list => {
        this.videos = list;
        this.loading = false;
        const first = list.find(v => v.id === wanted) || list.find(v => !v.progress?.completed) || list[0];
        if (first) { this.open(first, false); }
      },
      error: err => {
        this.loading = false;
        this.error = err.status === 403 ? (err.error?.message || 'Enroll in this course to watch its videos.')
          : err.status === 404 ? 'This course was not found.' : 'Could not load the videos. Please try again.';
      }
    });
  }

  ngOnDestroy(): void {
    this.saveNow();
    this.stopTimer();
  }

  open(v: CourseVideo, saveFirst = true): void {
    if (saveFirst) { this.saveNow(); }
    this.stopTimer();
    this.current = v;
    this.playError = '';
    this.retried = false;
    this.resumedAt = 0;
    const p = v.progress;
    this.startAt = p && !p.completed && p.positionSec > 3 ? p.positionSec : 0;
    this.loadSource(v);
  }

  private loadSource(v: CourseVideo): void {
    this.src = '';
    this.api.ticket(v.id).subscribe({
      next: t => { if (this.current?.id === v.id) { this.src = this.api.streamUrl(t); } },
      error: err => { this.playError = err.error?.message || 'This video could not be opened.'; }
    });
  }

  // ------- player events
  onMetadata(): void {
    const el = this.player?.nativeElement;
    if (!el || !this.current) { return; }
    if (this.startAt > 0 && this.startAt < (el.duration || Infinity)) {
      el.currentTime = this.startAt;
      this.resumedAt = this.startAt;
    }
  }

  onPlay(): void {
    if (!this.isCustomer || this.timer) { return; }
    this.timer = setInterval(() => this.saveNow(), this.saveEveryMs);
  }

  onPause(): void {
    this.stopTimer();
    this.saveNow();
  }

  onEnded(): void {
    this.stopTimer();
    this.saveNow();
    const i = this.videos.findIndex(v => v.id === this.current?.id);
    if (i >= 0 && i < this.videos.length - 1) { this.open(this.videos[i + 1], false); }
  }

  /** The link may have run out (tickets are short-lived): ask for a new one once and carry on from the same second. */
  onError(): void {
    const el = this.player?.nativeElement;
    if (!this.current || !this.src) { return; }
    if (!this.retried) {
      this.retried = true;
      this.startAt = el ? Math.floor(el.currentTime) : 0;
      this.loadSource(this.current);
    } else {
      this.playError = 'This video cannot be played. It may be missing or in a format your browser does not support.';
    }
  }

  /** Saves the position (customers only). Failures are ignored on purpose: the next save will try again. */
  saveNow(): void {
    const el = this.player?.nativeElement;
    const v = this.current;
    if (!this.isCustomer || !el || !v || !this.src) { return; }
    const pos = Math.floor(el.currentTime || 0);
    const dur = Math.floor(isFinite(el.duration) ? el.duration : 0);
    if (pos <= 0 && !v.progress) { return; }
    this.api.saveProgress(v.id, pos, dur).subscribe({
      next: p => { v.progress = p; },
      error: () => { /* try again at the next save */ }
    });
  }

  private stopTimer(): void {
    if (this.timer) { clearInterval(this.timer); this.timer = null; }
  }

  back(): void {
    this.router.navigate([this.isCustomer ? '/my-learning' : '/manage-videos'], this.isCustomer ? {} : { queryParams: { courseId: this.courseId } });
  }

  trackById(_: number, v: CourseVideo): number { return v.id; }
}
