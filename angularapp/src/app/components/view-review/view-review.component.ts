import { Component, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Review } from '../../models/review.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-view-review',
  templateUrl: './view-review.component.html',
  styleUrls: ['./view-review.component.css']
})
export class ViewReviewComponent implements OnInit {
  reviews: Review[] = [];
  errorMessage = '';
  loaded = false;

  // simple page-by-page view of the list (the whole list is already loaded)
  pageIndex = 0;
  pageSize = 10;

  get pageItems(): any[] {
    const start = this.pageIndex * this.pageSize;
    return this.reviews.slice(start, start + this.pageSize);
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  constructor(private cartService: CartService) {}

  ngOnInit(): void {
    this.cartService.getAllReviews().subscribe({
      next: (data) => { this.reviews = data; this.loaded = true; },
      error: () => { this.errorMessage = 'Could not load the reviews.'; this.loaded = true; }
    });
  }
}