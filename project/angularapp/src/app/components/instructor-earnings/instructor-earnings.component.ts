import { Component, OnInit } from '@angular/core';
import { EarningsSummary } from '../../models/billing.model';
import { EarningsService } from '../../services/earnings.service';

/** An instructor's sales: how much was earned, what waits for payout, what was already paid. */
@Component({
  selector: 'app-instructor-earnings',
  templateUrl: './instructor-earnings.component.html',
  styleUrls: ['./instructor-earnings.component.css']
})
export class InstructorEarningsComponent implements OnInit {
  summary: EarningsSummary | null = null;
  loaded = false;
  loadError = '';

  constructor(private api: EarningsService) {}

  ngOnInit(): void {
    this.api.mine().subscribe({
      next: s => { this.summary = s; this.loaded = true; },
      error: () => { this.loaded = true; this.loadError = 'Could not load your earnings. Please try again.'; }
    });
  }
}
