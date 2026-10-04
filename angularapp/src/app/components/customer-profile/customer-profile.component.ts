import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CustomerService } from '../../services/customer.service';
import { UserStoreService } from '../../services/user-store.service';

/**
 * View and update the customer's own profile. Email and role come from the login token and are read-only.
 *
 * @author Meruva Lokesh
 */
@Component({
  selector: 'app-customer-profile',
  templateUrl: './customer-profile.component.html',
  styleUrls: ['./customer-profile.component.css']
})
export class CustomerProfileComponent implements OnInit {
  profileForm: FormGroup;
  customerId: any = null;
  email = '';
  username = '';
  errorMessage = '';
  loading = false;
  loaded = false;

  constructor(
    private fb: FormBuilder,
    private customerService: CustomerService,
    private userStore: UserStoreService,
    private snack: MatSnackBar
  ) {
    this.profileForm = this.fb.group({
      customerName: ['', [Validators.required, Validators.minLength(2)]],
      mobileNumber: ['', [Validators.pattern(/^[0-9]{10}$/)]],
      information: ['', [Validators.required, Validators.maxLength(300)]]
    });
  }

  get f() { return this.profileForm.controls; }

  get initial(): string {
    return (this.f['customerName'].value || this.username || '?').charAt(0).toUpperCase();
  }

  ngOnInit(): void {
    const user = this.userStore.user;
    this.email = user?.email || '';
    this.username = user?.username || '';
    this.customerService.viewCustomerByUserId().subscribe({
      next: (c) => {
        this.customerId = c.customerId;
        this.profileForm.patchValue({
          customerName: c.customerName,
          information: c.information,
          mobileNumber: c.user?.mobileNumber || ''
        });
        this.loaded = true;
      },
      error: () => { this.errorMessage = 'Could not load your profile.'; this.loaded = true; }
    });
  }

  onSubmit(): void {
    if (this.profileForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const v = this.profileForm.value;
    this.customerService.updateCustomer(this.customerId, {
      customerName: v.customerName,
      information: v.information,
      mobileNumber: v.mobileNumber || undefined
    }).subscribe({
      next: () => { this.loading = false; this.profileForm.markAsPristine(); this.snack.open('Profile updated', 'OK', { duration: 3000 }); },
      error: () => { this.loading = false; this.errorMessage = 'Could not update your profile. Please try again.'; }
    });
  }
}
