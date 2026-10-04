import { Component, Input } from '@angular/core';

/**
 * Read-only star rating ("4.5 (12)"). Shows nothing but "No reviews yet" when a course has no reviews.
 *
 * @author Sumit
 */
@Component({
  selector: 'app-star-rating',
  template: `
    <span class="rating" *ngIf="count && count > 0; else none" [attr.aria-label]="value + ' out of 5 stars from ' + count + ' reviews'">
      <strong>{{ value | number:'1.1-1' }}</strong>
      <mat-icon *ngFor="let s of stars">{{ s <= roundedValue ? 'star' : (s - 0.5 <= value ? 'star_half' : 'star_border') }}</mat-icon>
      <span class="count">({{ count }})</span>
    </span>
    <ng-template #none><span class="none">No reviews yet</span></ng-template>
  `,
  styles: [`
    .rating { display: inline-flex; align-items: center; gap: 2px; color: #b46a05; font-size: 13px; }
    .rating mat-icon { width: 16px; height: 16px; font-size: 16px; color: #f59e0b; }
    .rating strong { margin-right: 3px; }
    .count, .none { color: #64748b; font-size: 12.5px; margin-left: 3px; }
  `]
})
export class StarRatingComponent {
  @Input() value = 0;
  @Input() count: number | undefined = 0;
  stars = [1, 2, 3, 4, 5];

  get roundedValue(): number {
    return Math.floor(this.value + 0.25);
  }
}
