import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CustomerService } from '../../services/customer.service';

@Component({
  selector: 'app-customerdashboard',
  templateUrl: './customerdashboard.component.html',
  styleUrls: ['./customerdashboard.component.css']
})
export class CustomerdashboardComponent {
  detailsForm: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(private fb: FormBuilder, private customerService: CustomerService, private router: Router) {
    this.detailsForm = this.fb.group({
      customerName: ['', [Validators.required]],
      information: ['', [Validators.required]]
    });
  }

  get f() { return this.detailsForm.controls; }

  onSubmit(): void {
    if (this.detailsForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const customer = {
      customerName: this.detailsForm.value.customerName,
      information: this.detailsForm.value.information,
      user: { userId: Number(localStorage.getItem('userId')) }
    };
    this.customerService.registerCustomer(customer).subscribe({
      next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          // details already exist - just load them and carry on
          this.customerService.viewCustomerByUserId().subscribe(() => this.router.navigate(['/dashboard']));
        } else {
          this.errorMessage = 'Could not save your details. Please try again.';
        }
      }
    });
  }
}
