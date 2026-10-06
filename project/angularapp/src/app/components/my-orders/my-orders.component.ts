import { Component, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-my-orders',
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.css']
})
export class MyOrdersComponent implements OnInit {
  orders: any[] = [];
  errorMessage = '';
  loaded = false;

  // simple page-by-page view of the list (the whole list is already loaded)
  pageIndex = 0;
  pageSize = 3;

  get pageItems(): any[] {
    const start = this.pageIndex * this.pageSize;
    return this.orders.slice(start, start + this.pageSize);
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  constructor(private orderService: OrderService, private router: Router) {}

  ngOnInit(): void {
    this.orderService.viewOrderByCustomerId().subscribe({
      next: (data) => { this.orders = data; this.loaded = true; },
      error: (err) => {
        this.loaded = true;
        if (err.status !== 404) { this.errorMessage = 'Could not load your enrollments.'; }
      }
    });
  }

  addReview(): void {
    this.router.navigate(['/add-review']);
  }
}
