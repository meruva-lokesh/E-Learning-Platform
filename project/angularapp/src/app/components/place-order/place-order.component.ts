import { Component, NgZone, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { Course } from '../../models/course.model';
import { CartService } from '../../services/cart.service';
import { CustomerService } from '../../services/customer.service';
import { OrderService } from '../../services/order.service';
import { PaymentService, RazorpayStop } from '../../services/payment.service';

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
  /** The order saved by a verified Razorpay payment; its invoice is shown after the confirmation. */
  private paidOrderId: number | null = null;

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private customerService: CustomerService,
    private orderService: OrderService,
    private router: Router,
    private dialog: MatDialog,
    private payment: PaymentService,
    private zone: NgZone
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

  /** True when the Payment page should use Razorpay (environment.razorpayEnabled). */
  get razorpayOn(): boolean { return this.payment.enabled; }

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
    // Razorpay switched on and something to pay: the server starts the payment, the order is saved only after it is verified
    if (this.razorpayOn && Number(this.orderForm.getRawValue().totalPrice) > 0) {
      this.payWithRazorpay();
      return;
    }
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
      error: (err) => {
        this.loading = false;
        // 409 = the server refused because a course in the cart was already bought
        this.errorMessage = err.status === 409
          ? (err.error?.message || 'You are already enrolled in one of these courses.')
          : 'Payment could not be completed. Please try again.';
      }
    });
  }

  // ---------- Razorpay (test mode or live, decided by the keys on the server) ----------

  private payWithRazorpay(): void {
    this.loading = true;
    this.errorMessage = '';
    this.payment.createRazorpayOrder().subscribe({
      next: (co) => this.openRazorpay(co),
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.status === 409
          ? (err.error?.message || 'You are already enrolled in one of these courses.')
          : err.status === 400
            ? (err.error?.message || 'Payment could not be started.')
            : 'Payment could not be started. Please try again.';
      }
    });
  }

  private async openRazorpay(co: any): Promise<void> {
    const form = this.orderForm.getRawValue();
    try {
      const success = await this.payment.openCheckout({
        key: co.keyId,
        amount: co.amount,
        currency: co.currency,
        name: co.name,
        description: co.description,
        order_id: co.razorpayOrderId,
        prefill: { name: form.name, email: form.email, contact: form.mobileNumber },
        // the amount is fixed by the server-created order; name, e-mail and mobile are filled in and cannot be edited in the pop-up
        readonly: { name: true, email: true, contact: true },
        theme: { color: '#f59e0b' }
      });
      this.zone.run(() => this.verifyRazorpay(success));
    } catch (e: any) {
      const stop = e as RazorpayStop;
      this.zone.run(() => {
        this.loading = false;
        if (stop?.reason === 'dismissed') {
          this.errorMessage = 'Payment was cancelled. You were not charged.';
        } else if (stop?.reason === 'failed') {
          this.errorMessage = 'Payment failed' + (stop.description ? ': ' + stop.description : '') + '. You can try again.';
        } else {
          this.errorMessage = 'Razorpay could not be loaded. Check your internet connection and try again.';
        }
      });
    }
  }

  private verifyRazorpay(success: any): void {
    this.payment.verifyRazorpayPayment(success).subscribe({
      next: (order) => {
        // the server saved the order and emptied the cart
        this.paidOrderId = order?.orderId ?? null;
        this.loading = false;
        this.showConfirmation();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = (err.error?.message || 'We could not confirm your payment.')
          + ' If money was deducted, contact support with payment id ' + success.razorpay_payment_id + '.';
      }
    });
  }

  private showConfirmation(): void {
    this.paymentDone = true;
    this.dialog.open(ConfirmDialogComponent, {
      disableClose: true,
      data: { title: 'Payment was successful!', message: this.paidOrderId ? 'You are now enrolled. Your invoice is ready and opens next.' : 'You are now enrolled. Find your courses under My Enrollments.', confirmText: 'OK', hideCancel: true, icon: 'check_circle' }
    }).afterClosed().subscribe(() => this.closeConfirmation());
  }

  closeConfirmation(): void {
    this.paymentDone = false;
    this.router.navigate(this.paidOrderId ? ['/invoice/order', this.paidOrderId] : ['/my-orders']);
  }
}