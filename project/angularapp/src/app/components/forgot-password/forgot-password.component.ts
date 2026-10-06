import { Component, OnDestroy } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { PasswordResetService } from '../../services/password-reset.service';
import { PASSWORD_RULES } from '../registration/registration.component';

function strong(c: AbstractControl): ValidationErrors | null {
  const v: string = c.value || '';
  return v && PASSWORD_RULES.some(r => !r.test(v)) ? { weak: true } : null;
}

/**
 * "Forgot password" without e-mail. Step 1: the user types the e-mail. Step 2: the user types a new password (and, only when
 * the server asks for it, the registered mobile number). The password is changed at once and the user can log in with it.
 */
@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent implements OnDestroy {
  step: 'email' | 'password' | 'done' = 'email';
  emailForm = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  passForm = this.fb.group({
    mobile: [''],
    password: ['', [Validators.required, strong]],
    confirmPassword: ['', Validators.required]
  }, { validators: (g: AbstractControl): ValidationErrors | null => g.get('password')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true } });
  email = '';
  requireMobile = false;
  showPassword = false;
  busy = false;
  error = '';
  notRegistered = false;
  secondsLeft = 0;
  private timer?: any;

  constructor(private fb: FormBuilder, private resetApi: PasswordResetService) {}

  ngOnDestroy(): void { clearInterval(this.timer); }

  get emailCtl() { return this.emailForm.get('email'); }
  get f() { return this.passForm.controls; }

  get checks() {
    const v: string = this.f['password'].value || '';
    return PASSWORD_RULES.map(r => ({ label: r.label, ok: r.test(v) }));
  }

  /** Step 1 -> step 2. */
  next(): void {
    if (this.emailForm.invalid || this.busy || this.secondsLeft > 0) { this.emailForm.markAllAsTouched(); return; }
    this.busy = true;
    this.error = '';
    this.resetApi.start(String(this.emailForm.value.email).trim()).subscribe({
      next: r => {
        this.busy = false;
        this.email = String(this.emailForm.value.email).trim().toLowerCase();
        this.requireMobile = !!r.requireMobile;
        const mobile = this.f['mobile'];
        mobile.setValidators(this.requireMobile ? [Validators.required, Validators.pattern(/^[0-9]{10}$/)] : []);
        mobile.updateValueAndValidity();
        this.step = 'password';
      },
      error: err => this.fail(err, 'Could not continue. Please try again in a moment.')
    });
  }

  /** Step 2: change the password. */
  save(): void {
    if (this.passForm.invalid || this.busy || this.secondsLeft > 0) { this.passForm.markAllAsTouched(); return; }
    this.busy = true;
    this.error = '';
    this.notRegistered = false;
    const mobile = this.requireMobile ? String(this.f['mobile'].value || '').trim() : undefined;
    this.resetApi.reset(this.email, String(this.f['password'].value), mobile).subscribe({
      next: () => { this.busy = false; this.step = 'done'; },
      error: err => {
        this.notRegistered = err.status === 404;
        this.fail(err, 'The password could not be changed. Please try again in a moment.');
      }
    });
  }

  backToEmail(): void {
    this.step = 'email';
    this.error = '';
    this.notRegistered = false;
  }

  private fail(err: any, fallback: string): void {
    this.busy = false;
    this.error = [400, 404, 429].includes(err.status) ? (err.error?.message || fallback) : fallback;
    if (err.status === 429) { this.startCountdown(Number(err.headers?.get('Retry-After')) || 60); }
  }

  private startCountdown(seconds: number): void {
    clearInterval(this.timer);
    this.secondsLeft = seconds;
    this.timer = setInterval(() => {
      this.secondsLeft = Math.max(0, this.secondsLeft - 1);
      if (this.secondsLeft === 0) { clearInterval(this.timer); }
    }, 1000);
  }
}
