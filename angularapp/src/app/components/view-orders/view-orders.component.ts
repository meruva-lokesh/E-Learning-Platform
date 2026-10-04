import { Component, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-view-orders',
  templateUrl: './view-orders.component.html',
  styleUrls: ['./view-orders.component.css']
})
export class ViewOrdersComponent implements OnInit {
  orders: any[] = [];
  errorMessage = '';
  loaded = false;

  // simple page-by-page view of the list (the whole list is already loaded)
  pageIndex = 0;
  pageSize = 10;

  get pageItems(): any[] {
    const start = this.pageIndex * this.pageSize;
    return this.orders.slice(start, start + this.pageSize);
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.orderService.viewAllOrders().subscribe({
      next: (data) => { this.orders = data; this.loaded = true; },
      error: () => { this.errorMessage = 'Could not load the enrollments.'; this.loaded = true; }
    });
  }
}