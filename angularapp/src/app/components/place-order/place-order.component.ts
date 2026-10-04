import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { Course } from '../../models/course.model';
import { CartService } from '../../services/cart.service';
import { CustomerService } from '../../services/customer.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-place-order',
  templateUrl: './place-order.component.html',
  styleUrls: ['./place-order.component.css']
})
export class PlaceOrderComponent implements OnInit {
  orderForm: FormGroup;
  courses: Course[] = [];
  errorMessage = '';
  loading = false;
  paymentDone = false;

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private customerService: CustomerService,
    private orderService: OrderService,
    private router: Router,
    private dialog: MatDialog
  ) {
    this.orderForm = this.fb.group({
      name: ['', [Validators.required]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      information: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      totalPrice: [{ value: 0, disabled: true }]
    });
  }

  get f() { return this.orderForm.controls; }

  ngOnInit(): void {
    this.cartService.getAllCoursesFromCart().subscribe({
      next: (cart) => {
        this.courses = cart.courses || [];
        this.orderForm.patchValue({ totalPrice: cart.totalAmount || 0 });
      },
      error: () => (this.errorMessage = 'Could not load your cart.')
    });
    this.customerService.viewCustomerByUserId().subscribe({
      next: (customer) => this.orderForm.patchValue({
        name: customer.customerName,
        information: customer.information,
        mobileNumber: customer.user?.mobileNumber,
        email: customer.user?.email
      })
    });
  }

  makePayment(): void {
    if (this.orderForm.invalid || this.courses.length === 0 || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const order = {
      customer: { customerId: Number(localStorage.getItem('customerId')) },
      courses: this.courses.map(c => ({ courseId: c.courseId })),
      orderPrice: this.orderForm.getRawValue().totalPrice
    };
    this.orderService.addOrder(order).subscribe({
      next: () => {
        // the order is saved, so empty the cart before confirming
        this.cartService.removeAllCourses().subscribe({
          next: () => { this.loading = false; this.showConfirmation(); },
          error: () => { this.loading = false; this.showConfirmation(); }
        });
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Payment could not be completed. Please try again.';
      }
    });
  }

  private showConfirmation(): void {
    this.paymentDone = true;
    this.dialog.open(ConfirmDialogComponent, {
      disableClose: true,
      data: { title: 'Payment was successful!', message: 'You are now enrolled. Find your courses under My Enrollments.', confirmText: 'OK', hideCancel: true, icon: 'check_circle' }
    }).afterClosed().subscribe(() => this.closeConfirmation());
  }

  closeConfirmation(): void {
    this.paymentDone = false;
    this.router.navigate(['/my-orders']);
  }
}
