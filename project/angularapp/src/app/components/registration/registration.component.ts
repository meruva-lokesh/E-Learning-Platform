import { AfterViewInit, Component, ElementRef, NgZone, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, FormGroupDirective, NgForm, ValidationErrors, Validators } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OtpService } from '../../services/otp.service';
import { CustomerService } from '../../services/customer.service';
import { GoogleAuthService } from '../../services/google-auth.service';

/** Username: no spaces anywhere. */
function noSpaces(control: AbstractControl): ValidationErrors | null {
  return /\s/.test(control.value || '') ? { spaces: true } : null;
}

/** Password: one upper case, one lower case, one number, one special character, and no spaces. */
export const PASSWORD_RULES = [
  { key: 'length', label: '8 to 72 characters', test: (v: string) => v.length >= 8 && v.length <= 72 },
  { key: 'upper', label: 'One upper case letter', test: (v: string) => /[A-Z]/.test(v) },
  { key: 'lower', label: 'One lower case letter', test: (v: string) => /[a-z]/.test(v) },
  { key: 'digit', label: 'One number', test: (v: string) => /[0-9]/.test(v) },
  { key: 'special', label: 'One special character (for example @ # $ % !)', test: (v: string) => /[^A-Za-z0-9\s]/.test(v) },
  { key: 'space', label: 'No spaces', test: (v: string) => v.length > 0 && !/\s/.test(v) }
];

function strongPassword(control: AbstractControl): ValidationErrors | null {
  const v: string = control.value || '';
  return v && PASSWORD_RULES.some(r => !r.test(v)) ? { weak: true } : null;
}

/** Email: name, one @, then a domain with a dot followed by at least 2 letters (a@b.com works, a@b does not). */
const EMAIL_PATTERN = /^[A-Za-z0-9._%+\-]+@[A-Za-z0-9\-]+(\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}$/;

/** Mobile: 10 digits, the first one is 6, 7, 8 or 9. */
const MOBILE_PATTERN = /^[6-9][0-9]{9}$/;

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
export class RegistrationComponent implements OnInit, OnDestroy, AfterViewInit {
  registerForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;
  passwordFocused = false;
  confirmMatcher = new ConfirmMatcher();

  // ---- mobile OTP, shown inside this page (step 2) ----
  /** null = not known yet, true = the server asks for OTP, false = OTP is switched off on the server. */
  otpRequired: boolean | null = null;
  step: 'form' | 'verify' | 'done' = 'form';
  email = '';
  maskedMobile = '';
  code = '';
  sending = false;
  verifying = false;
  otpError = '';
  otpInfo = '';
  devCode = '';
  secondsLeft = 0;
  private timer?: any;

  // ---- sign up with Google (OAuth): the account is created and stored in the database ----
  googleEnabled = false;
  googleLoading = false;
  googleError = '';
  /** True when the Google e-mail is already registered: the page then offers a link to log in. */
  alreadyRegistered = false;
  @ViewChild('googleButton') googleButton?: ElementRef<HTMLElement>;

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router, private otp: OtpService,
              private google: GoogleAuthService, private customerService: CustomerService, private zone: NgZone) {
    this.googleEnabled = this.google.enabled;
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), noSpaces]],
      email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]],
      password: ['', [Validators.required, strongPassword]],
      confirmPassword: ['', [Validators.required]],
      mobileNumber: ['', [Validators.required, Validators.pattern(MOBILE_PATTERN)]],
      role: ['Customer']   // fixed: public sign-up always creates a customer
    }, { validators: passwordsMatch });
  }

  ngOnInit(): void {
    // only to show a short note and the step list; failing here changes nothing
    this.otp.status().subscribe({ next: s => (this.otpRequired = !!s.required), error: () => (this.otpRequired = null) });
  }

  ngAfterViewInit(): void {
    if (this.googleEnabled && this.googleButton) {
      this.google.renderButton(this.googleButton.nativeElement, (credential) => this.onGoogleCredential(credential), 'signup_with')
        .catch(() => { this.googleError = 'Google sign-up could not be loaded. Check your internet connection.'; });
    }
  }

  ngOnDestroy(): void { clearInterval(this.timer); }

  /** Google calls this outside Angular. mode "register" makes the server store the account (name and e-mail from Google). */
  private onGoogleCredential(credential: string): void {
    this.zone.run(() => {
      if (this.googleLoading) { return; }
      this.googleLoading = true;
      this.googleError = '';
      this.alreadyRegistered = false;
      this.auth.loginWithGoogle(credential, 'register').subscribe({
        next: () => {
          // the account now exists and the user is logged in; a customer fills in his details once on the next page
          this.googleLoading = false;
          this.customerService.viewCustomerByUserId().subscribe({
            next: () => this.router.navigate(['/dashboard']),
            error: () => this.router.navigate(['/customerdashboard'])
          });
        },
        error: (err) => {
          this.googleLoading = false;
          if (err.status === 409) {
            this.alreadyRegistered = true;
            this.googleError = 'This Google e-mail is already registered. Please log in instead.';
          } else if (err.status === 401) {
            this.googleError = 'Google sign-up failed. Please try again.';
          } else if (err.status === 400) {
            this.googleError = 'Sign up with Google is not set up on the server yet.';
          } else {
            this.googleError = 'Unable to reach the server. Please try again later.';
          }
        }
      });
    });
  }

  get f() { return this.registerForm.controls; }

  /** Live checklist under the password field. */
  get passwordChecks() {
    const v: string = this.f['password'].value || '';
    return PASSWORD_RULES.map(r => ({ label: r.label, ok: r.test(v) }));
  }

  onSubmit(): void {
    if (this.registerForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const { confirmPassword, ...user } = this.registerForm.value;
    this.auth.register(user).subscribe({
      next: () => this.afterRegister(user.email, user.mobileNumber),
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

  /** Account created. If the server asks for OTP, stay on this page and show step 2; otherwise go to login as before. */
  private afterRegister(email: string, mobile: string): void {
    this.otp.status().subscribe({
      next: s => {
        this.loading = false;
        this.otpRequired = !!s.required;
        if (!s.required) { this.router.navigate(['/login']); return; }
        this.email = email;
        this.maskedMobile = '******' + String(mobile).slice(-4);
        this.step = 'verify';
        this.sendCode();
      },
      error: () => { this.loading = false; this.router.navigate(['/login']); }
    });
  }

  get canVerify(): boolean {
    return /^[0-9]{6}$/.test(this.code) && !this.verifying;
  }

  onCodeInput(): void {
    this.code = this.code.replace(/[^0-9]/g, '').slice(0, 6);
  }

  sendCode(): void {
    if (this.sending || this.secondsLeft > 0) { return; }
    this.sending = true;
    this.otpError = '';
    this.otp.send(this.email).subscribe({
      next: r => {
        this.sending = false;
        this.otpInfo = r.message;
        this.devCode = r.devCode || '';
        this.startCountdown(r.resendInSeconds || 30);
      },
      error: err => {
        this.sending = false;
        this.otpError = [400, 429].includes(err.status) ? (err.error?.message || 'Could not send the code.')
          : 'The code could not be sent. Please try again in a moment.';
        if (err.status === 429) { this.startCountdown(Number(err.headers?.get('Retry-After')) || 30); }
      }
    });
  }

  verify(): void {
    if (!this.canVerify) { return; }
    this.verifying = true;
    this.otpError = '';
    this.otp.verify(this.email, this.code).subscribe({
      next: () => { this.verifying = false; clearInterval(this.timer); this.step = 'done'; },
      error: err => {
        this.verifying = false;
        this.code = '';
        this.otpError = [400, 429].includes(err.status) ? (err.error?.message || 'That code did not work.')
          : 'Could not check the code. Please try again.';
      }
    });
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
