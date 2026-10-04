import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, FormGroupDirective, NgForm, ValidationErrors, Validators } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

/** Shows the "Password do not match" error on the confirm field, even though the rule lives on the whole form. */
class ConfirmMatcher implements ErrorStateMatcher {
  isErrorState(control: AbstractControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!control && (control.touched || control.dirty) && (control.invalid || !!form?.form.hasError('mismatch'));
  }
}

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent {
  registerForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;
  confirmMatcher = new ConfirmMatcher();

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)]],
      confirmPassword: ['', [Validators.required]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      role: ['Customer']   // fixed: public sign-up always creates a customer
    }, { validators: passwordsMatch });
  }

  get f() { return this.registerForm.controls; }

  onSubmit(): void {
    if (this.registerForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const { confirmPassword, ...user } = this.registerForm.value;
    this.auth.register(user).subscribe({
      next: () => { this.loading = false; this.router.navigate(['/login']); },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          this.errorMessage = 'A user with this email already exists';
        } else if (err.status === 429) {
          this.errorMessage = 'Too many attempts. Please wait a minute and try again.';
        } else if (err.status === 400) {
          this.errorMessage = err.error?.message || 'Please check the entered details';
        } else {
          this.errorMessage = 'Registration failed. Please try again later.';
        }
      }
    });
  }
}