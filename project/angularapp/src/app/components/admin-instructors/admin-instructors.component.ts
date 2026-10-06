import { Component, OnInit } from '@angular/core';
import { InstructorService } from '../../services/instructor.service';
import { ApplicationStatus, InstructorView } from '../../models/instructor.model';

type Tab = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL';

/**
 * Admin page: every instructor application with its details. The admin can approve it, or reject it with a reason that the
 * instructor will read on their own application page.
 */
@Component({
  selector: 'app-admin-instructors',
  templateUrl: './admin-instructors.component.html',
  styleUrls: ['./admin-instructors.component.css']
})
export class AdminInstructorsComponent implements OnInit {
  readonly tabs: { key: Tab; label: string }[] = [
    { key: 'PENDING', label: 'Pending' }, { key: 'APPROVED', label: 'Approved' }, { key: 'REJECTED', label: 'Rejected' }, { key: 'ALL', label: 'All' }
  ];
  tab: Tab = 'PENDING';
  all: InstructorView[] = [];
  loaded = false;
  loadError = '';
  busyId: number | null = null;
  rejectingId: number | null = null;
  reason = '';
  actionError = '';
  actionOk = '';

  constructor(private instructors: InstructorService) {}

  ngOnInit(): void {
    this.load();
  }

  get shown(): InstructorView[] {
    return this.tab === 'ALL' ? this.all : this.all.filter(i => i.status === this.tab);
  }

  count(tab: Tab): number {
    return tab === 'ALL' ? this.all.length : this.all.filter(i => i.status === tab).length;
  }

  get reasonLength(): number {
    return this.reason.trim().length;
  }

  get reasonOk(): boolean {
    return this.reasonLength >= 5 && this.reasonLength <= 500;
  }

  load(): void {
    this.loadError = '';
    this.instructors.list('ALL').subscribe({
      next: list => { this.all = list; this.loaded = true; },
      error: () => { this.loaded = true; this.loadError = 'Could not load the applications. Please try again.'; }
    });
  }

  approve(i: InstructorView): void {
    if (this.busyId !== null) { return; }
    this.busyId = i.userId;
    this.actionError = '';
    this.actionOk = '';
    this.instructors.approve(i.userId).subscribe({
      next: u => { this.replace(u); this.busyId = null; this.rejectingId = null; this.actionOk = u.username + ' is approved.'; },
      error: err => { this.busyId = null; this.actionError = err.error?.message || 'Could not approve. Please try again.'; }
    });
  }

  startReject(i: InstructorView): void {
    this.rejectingId = i.userId;
    this.reason = '';
    this.actionError = '';
    this.actionOk = '';
  }

  cancelReject(): void {
    this.rejectingId = null;
    this.reason = '';
  }

  confirmReject(i: InstructorView): void {
    if (!this.reasonOk || this.busyId !== null) { return; }
    this.busyId = i.userId;
    this.actionError = '';
    this.instructors.reject(i.userId, this.reason.trim()).subscribe({
      next: u => { this.replace(u); this.busyId = null; this.rejectingId = null; this.reason = ''; this.actionOk = u.username + ' was rejected. The reason is saved.'; },
      error: err => { this.busyId = null; this.actionError = err.error?.message || 'Could not reject. Please try again.'; }
    });
  }

  chip(status: ApplicationStatus): string {
    return status === 'APPROVED' ? 'ok' : status === 'REJECTED' ? 'bad' : 'wait';
  }

  trackById(_: number, i: InstructorView): number {
    return i.userId;
  }

  private replace(u: InstructorView): void {
    this.all = this.all.map(x => (x.userId === u.userId ? u : x));
  }
}
