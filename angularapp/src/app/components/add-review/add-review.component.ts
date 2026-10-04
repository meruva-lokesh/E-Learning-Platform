import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormBuilder, FormGroup, FormGroupDirective, Validators } from '@angular/forms';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-add-review',
  templateUrl: './add-review.component.html',
  styleUrls: ['./add-review.component.css']
})
export class AddReviewComponent implements OnInit {
  reviewForm: FormGroup;
  courseTypes: string[] = [];
  stars = [1, 2, 3, 4, 5];
  customerId = localStorage.getItem('customerId') || '';
  message = '';
  errorMessage = '';
  loading = false;

  constructor(private fb: FormBuilder, private cartService: CartService, private orderService: OrderService, private snack: MatSnackBar) {
    this.reviewForm = this.fb.group({
      subject: ['', [Validators.required]],
      body: ['', [Validators.required]],
      rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]]
    });
  }

  get f() { return this.reviewForm.controls; }

  ngOnInit(): void {
    // only courses the customer has enrolled in can be reviewed
    this.orderService.viewOrderByCustomerId().subscribe({
      next: (orders) => {
        const types = new Set<string>();
        orders.forEach((o: any) => (o.courses || []).forEach((c: any) => types.add(c.courseType)));
        this.courseTypes = Array.from(types);
      },
      error: () => (this.courseTypes = [])
    });
  }

  setRating(value: number): void {
    this.reviewForm.patchValue({ rating: value });
    this.reviewForm.get('rating')?.markAsTouched();
  }

  onSubmit(formDirective: FormGroupDirective): void {
    if (this.reviewForm.invalid || this.loading) { return; }
    this.loading = true;
    this.message = '';
    this.errorMessage = '';
    const review = { ...this.reviewForm.value, customer: { customerId: Number(this.customerId) } };
    this.cartService.addReview(review).subscribe({
      next: () => {
        this.loading = false;
        this.message = 'Review added successfully!';
        this.snack.open(this.message, 'OK', { duration: 3000 });
        formDirective.resetForm({ subject: '', body: '', rating: 0 });
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Could not submit your review. Please try again.';
      }
    });
  }
}