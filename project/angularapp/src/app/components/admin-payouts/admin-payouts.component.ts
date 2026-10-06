import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { PayableRow, PayoutView } from '../../models/billing.model';
import { EarningsService } from '../../services/earnings.service';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';

/**
 * Admin: who is owed money. The admin pays the instructor outside the app (bank or UPI), types the transaction
 * reference here and presses Mark as paid. That moves every pending sale of that instructor to Paid.
 */
@Component({
  selector: 'app-admin-payouts',
  templateUrl: './admin-payouts.component.html',
  styleUrls: ['./admin-payouts.component.css']
})
export class AdminPayoutsComponent implements OnInit {
  payable: PayableRow[] = [];
  history: PayoutView[] = [];
  refs: { [instructorUserId: number]: string } = {};
  loaded = false;
  loadError = '';
  actionError = '';
  actionOk = '';
  busyId: number | null = null;

  constructor(private api: EarningsService, private dialog: MatDialog) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    forkJoin({ payable: this.api.payable(), history: this.api.history() }).subscribe({
      next: r => { this.payable = r.payable; this.history = r.history; this.loaded = true; },
      error: () => { this.loaded = true; this.loadError = 'Could not load the payouts. Please try again.'; }
    });
  }

  refOk(r: PayableRow): boolean {
    const v = (this.refs[r.instructorUserId] || '').trim();
    return v.length >= 3 && v.length <= 100;
  }

  markPaid(r: PayableRow): void {
    if (!this.refOk(r) || this.busyId !== null) { return; }
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Mark as paid?',
        message: 'You are recording that ' + r.name + ' was paid ' + (r.pendingPaise / 100).toFixed(2) + ' rupees. Reference: ' + this.refs[r.instructorUserId].trim() + '.',
        confirmText: 'Mark as paid', cancelText: 'Cancel', icon: 'payments'
      }
    }).afterClosed().subscribe(yes => { if (yes) { this.pay(r); } });
  }

  private pay(r: PayableRow): void {
    this.busyId = r.instructorUserId;
    this.actionError = '';
    this.actionOk = '';
    this.api.pay(r.instructorUserId, this.refs[r.instructorUserId].trim()).subscribe({
      next: p => {
        this.busyId = null;
        this.actionOk = 'Recorded: ' + r.name + ' was paid ' + (p.amountPaise / 100).toFixed(2) + ' rupees.';
        delete this.refs[r.instructorUserId];
        this.load();
      },
      error: err => {
        this.busyId = null;
        this.actionError = err.error?.message || 'Could not record the payout. Please try again.';
      }
    });
  }

  trackByUser(_: number, r: PayableRow): number { return r.instructorUserId; }
}
