import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

/**
 * Shows one enrollment (order): its courses and the total paid.
 *
 * @author Tanvi
 */
@Component({
  selector: 'app-order-details',
  templateUrl: './order-details.component.html',
  styleUrls: ['./order-details.component.css']
})
export class OrderDetailsComponent implements OnInit {
  order?: Order;
  errorMessage = '';
  loaded = false;

  constructor(private route: ActivatedRoute, private orderService: OrderService) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.orderService.viewOrderById(id).subscribe({
      next: (order) => { this.order = order; this.loaded = true; },
      error: () => { this.errorMessage = 'This order could not be found.'; this.loaded = true; }
    });
  }
}
