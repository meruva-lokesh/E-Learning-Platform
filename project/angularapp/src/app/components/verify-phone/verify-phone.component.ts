import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OtpService } from '../../services/otp.service';

/**
 * "Verify your mobile number" page. It is opened after registration (and when a login is refused because the number
 * is not verified yet) with ?email=... in the address. If the platform does not ask for OTP it sends the user
 * straight to the login page. Otherwise it sends a code once, lets the user type the 6 digits, and offers
 * "Resend" after a short wait.
 */
@Component({
  selector: 'app-verify-phone',
  templateUrl: './verify-phone.component.html',
  styleUrls: ['./verify-phone.component.css']
})
export class VerifyPhoneComponent implements OnInit, OnDestroy {
  email = '';
  code = '';
  checking = true;
  sending = false;
  verifying = false;
  done = false;
  error = '';
  info = '';
  devCode = '';
  secondsLeft = 0;
  private timer?: any;
  private redirect?: any;

  constructor(private otp: OtpService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    this.email = (this.route.snapshot.queryParamMap.get('email') || '').trim();
    if (!this.email) { this.router.navigate(['/login']); return; }
    this.otp.status().subscribe({
      next: s => {
        if (!s.required) { this.router.navigate(['/login']); return; }
        this.checking = false;
        this.sendCode();
      },
      error: () => { this.checking = false; this.error = 'Could not reach the server. Please try again later.'; }
    });
  }

  ngOnDestroy(): void {
    clearInterval(this.timer);
    clearTimeout(this.redirect);
  }

  get canVerify(): boolean {
    return /^[0-9]{6}$/.test(this.code) && !this.verifying && !this.done;
  }

  onCodeInput(): void {
    this.code = this.code.replace(/[^0-9]/g, '').slice(0, 6);
  }

  sendCode(): void {
    if (this.sending || this.secondsLeft > 0) { return; }
    this.sending = true;
    this.error = '';
    this.otp.send(this.email).subscribe({
      next: r => {
        this.sending = false;
        this.info = r.message;
        this.devCode = r.devCode || '';
        this.startCountdown(r.resendInSeconds || 30);
      },
      error: err => {
        this.sending = false;
        this.error = [400, 429].includes(err.status) ? (err.error?.message || 'Could not send the code.')
          : 'The code could not be sent. Please try again in a moment.';
        if (err.status === 429) { this.startCountdown(Number(err.headers?.get('Retry-After')) || 30); }
      }
    });
  }

  verify(): void {
    if (!this.canVerify) { return; }
    this.verifying = true;
    this.error = '';
    this.otp.verify(this.email, this.code).subscribe({
      next: () => {
        this.verifying = false;
        this.done = true;
        clearInterval(this.timer);
        this.redirect = setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: err => {
        this.verifying = false;
        this.code = '';
        this.error = [400, 429].includes(err.status) ? (err.error?.message || 'That code did not work.')
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
