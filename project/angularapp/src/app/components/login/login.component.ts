import { AfterViewInit, Component, ElementRef, NgZone, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CustomerService } from '../../services/customer.service';
import { GoogleAuthService } from '../../services/google-auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements AfterViewInit {
  loginForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;
  /** True when Google said this e-mail has no account: the page then offers a link to register. */
  notRegistered = false;
  /**
   * True when the server refused an instructor whose application is pending or rejected:
   * the page links to the status check.
   */
  notApproved = false;
  /** True when a Google client id is configured, so the Google button is shown. */
  googleEnabled = false;
  @ViewChild('googleButton') googleButton?: ElementRef<HTMLElement>;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private customerService: CustomerService,
    private router: Router,
    private google: GoogleAuthService,
    private zone: NgZone
  ) {
    this.googleEnabled = this.google.enabled;
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }

  onSubmit(): void {
    if (this.loginForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    this.notApproved = false;
    this.auth.login(this.loginForm.value).subscribe({
      next: (res: any) => {
        if (res.role === 'INSTRUCTOR') {
          this.loading = false;
          this.router.navigate(['/instructor']);
          return;
        }
        if (res.role === 'ADMIN') {
          this.loading = false;
          this.router.navigate(['/dashboard']);
          return;
        }
        // customers must have filled in their details once before they can use the app
        this.customerService.viewCustomerByUserId().subscribe({
          next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
          error: (err) => {
            this.loading = false;
            if (err.status === 404) {
              this.router.navigate(['/customerdashboard']);
            } else {
              this.errorMessage = 'Could not load your profile. Please try again.';
            }
          }
        });
      },
      error: (err) => {
        this.loading = false;
        this.notApproved = false;
        if (err.status === 403 && String(err.error?.message || '').startsWith('Your instructor application')) {
          this.notApproved = true;
          this.errorMessage = err.error.message;
        } else if (err.status === 401) {
          this.errorMessage = 'Invalid email or password';
        } else if (err.status === 403 && String(err.error?.message || '').includes('verify your mobile number')) {
          this.router.navigate(['/verify-phone'], { queryParams: { email: this.loginForm.value.email } });
        } else if (err.status === 429) {
          this.errorMessage = 'Too many attempts. Please wait a while and try again.';
        } else {
          this.errorMessage = 'Unable to reach the server. Please try again later.';
        }
      }
    });
  }

  // ---------- Login with Google (OAuth 2.0) ----------

  ngAfterViewInit(): void {
    if (this.googleEnabled && this.googleButton) {
      this.google.renderButton(this.googleButton.nativeElement, (credential) => this.onGoogleCredential(credential), 'signin_with')
        .catch(() => { this.errorMessage = 'Google sign-in could not be loaded. Check your internet connection.'; });
    }
  }

  /**
   * Reads the e-mail out of Google's ID token (a JWT) only to pre-fill the verify page.
   * The server never trusts this.
   */
  private emailFromGoogleCredential(credential: string): string {
    try {
      const part = credential.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const email = JSON.parse(atob(part)).email;
      return typeof email === 'string' ? email : '';
    } catch {
      return '';
    }
  }

  /** Google calls this outside Angular, so we step back into the Angular zone first. */
  private onGoogleCredential(credential: string): void {
    this.zone.run(() => {
      if (this.loading) { return; }
      this.loading = true;
      this.errorMessage = '';
      this.notRegistered = false;
      this.notApproved = false;
      this.auth.loginWithGoogle(credential, 'login').subscribe({
        next: (res: any) => this.goAfterLogin(res),
        error: (err) => {
          this.loading = false;
          if (err.status === 404) {
            // the Google e-mail is not registered: nothing was created
            this.notRegistered = true;
            this.errorMessage = 'No account found for this Google e-mail. Please register first.';
          } else if (err.status === 401) {
            this.errorMessage = 'Google sign-in failed. Please try again.';
          } else if (err.status === 403 && String(err.error?.message || '').includes('verify your mobile number')) {
            // registered with the form earlier and never verified the phone: finish that first
            const email = this.emailFromGoogleCredential(credential);
            if (email) {
              this.router.navigate(['/verify-phone'], { queryParams: { email } });
            } else {
              this.errorMessage = 'Please verify your mobile number first. Log in with your email and password to start.';
            }
          } else if (err.status === 403 && String(err.error?.message || '').startsWith('Your instructor application')) {
            this.notApproved = true;
            this.errorMessage = err.error.message;
          } else if (err.status === 403) {
            this.errorMessage = 'Admin accounts must log in with their email and password.';
          } else if (err.status === 400) {
            this.errorMessage = 'Login with Google is not set up on the server yet.';
          } else {
            this.errorMessage = 'Unable to reach the server. Please try again later.';
          }
        }
      });
    });
  }

  /**
   * Same rule as the normal login: admins go to the dashboard,
   * customers need their details saved once.
   */
  private goAfterLogin(res: any): void {
    if (res.role === 'INSTRUCTOR') {
      this.loading = false;
      this.router.navigate(['/instructor']);
      return;
    }
    if (res.role === 'ADMIN') {
      this.loading = false;
      this.router.navigate(['/dashboard']);
      return;
    }
    this.customerService.viewCustomerByUserId().subscribe({
      next: () => { this.loading = false; this.router.navigate(['/dashboard']); },
      error: (err) => {
        this.loading = false;
        if (err.status === 404) {
          this.router.navigate(['/customerdashboard']);
        } else {
          this.errorMessage = 'Could not load your profile. Please try again.';
        }
      }
    });
  }
}