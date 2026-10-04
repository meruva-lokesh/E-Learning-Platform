import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Course } from '../../models/course.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-my-cart',
  templateUrl: './my-cart.component.html',
  styleUrls: ['./my-cart.component.css']
})
export class MyCartComponent implements OnInit {
  courses: Course[] = [];
  totalAmount = 0;
  errorMessage = '';
  loaded = false;

  constructor(private cartService: CartService, private router: Router) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.cartService.getAllCoursesFromCart().subscribe({
      next: (cart) => {
        this.courses = cart.courses || [];
        this.totalAmount = cart.totalAmount || 0;
        this.loaded = true;
      },
      error: (err) => {
        // 404 simply means the customer has not added anything yet
        this.courses = [];
        this.totalAmount = 0;
        this.loaded = true;
        if (err.status !== 404) { this.errorMessage = 'Could not load your cart.'; }
      }
    });
  }

  remove(course: Course): void {
    this.cartService.removeCoursesFromCart(course.courseId).subscribe({
      next: () => this.load(),
      error: () => (this.errorMessage = 'Could not remove the course.')
    });
  }

  enrollNow(): void {
    if (this.courses.length > 0) { this.router.navigate(['/place-order']); }
  }
}
