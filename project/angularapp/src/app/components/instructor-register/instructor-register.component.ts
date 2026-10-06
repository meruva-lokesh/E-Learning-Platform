import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { InstructorService } from '../../services/instructor.service';
import { OtpService } from '../../services/otp.service';
import { ApplicationStatusView, INSTRUCTOR_ROLE } from '../../models/instructor.model';
type Mode = 'loading' | 'ready' | 'other-role';
type Tab = 'apply' | 'check';
type StepState = 'done' | 'now' | 'todo' | 'ok' | 'bad';
interface TrackerStep { title: string; detail: string; state: StepState; icon: string; }
/**
* One public page, two tabs.
* - "New application": the instructor registration form, then (when the server asks for it) the mobile
OTP step.
* - "Already applied?": the person types a username or an e-mail and sees how far the application has
come
* (submitted, mobile verified, under review, decision, login access). A rejected or pending application
can be fixed
* and sent again here with the e-mail and password, because instructors cannot log in before they are
approved.
* A logged-in approved instructor is sent to the instructor home page instead.
*/
@Component({
  selector: 'app-instructor-register',
  templateUrl: './instructor-register.component.html',
  styleUrls: ['./instructor-register.component.css']
})
export class InstructorRegisterComponent implements OnInit, OnDestroy {
  mode: Mode = 'loading';
  tab: Tab = 'apply';
  loading = false;
  errorMessage = '';
  showPassword = false;
  otherRole = '';
  // ---- registration, then the mobile OTP step ----
  /** null = not known yet, true = the server asks for OTP, false = OTP is switched off on the server. */
  otpRequired: boolean | null = null;
  regStep: 'form' | 'verify' | 'done' = 'form';
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
  // ---- "Already applied?" ----
  checkForm = this.fb.group({ identifier: ['', [Validators.required, Validators.maxLength(120)]] });
  checking = false;
  checkError = '';
  notFound = false;
  result: ApplicationStatusView | null = null;
  /** What the person typed for the last successful check (an e-mail is needed to fix the application). */
  lookedUp = '';
  checkWait = 0;
  private checkTimer?: any;
  // ---- fixing a pending or rejected application without logging in ----
  editing = false;
  saving = false;
  editError = '';
  resubmitted = false;
  private detailsControls = {
    qualification: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    experienceYears: [null as number | null, [Validators.required, Validators.min(0), Validators.max(60)]],
    expertise: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
    bio: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1000)]],
    profileLink: ['', [Validators.pattern(/^$|^https?:\/\/\S+$/), Validators.maxLength(300)]]
  };
  registerForm = this.fb.group({
    username: ['', [Validators.required, Validators.maxLength(50), Validators.pattern(/^\S+$/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)]],
    confirmPassword: ['', Validators.required],
    mobileNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
    ...this.detailsControls
  }, {
    validators: (g: AbstractControl): ValidationErrors | null =>
      g.get('password')?.value === g.get('confirmPassword')?.value ? null : { mismatch: true }
  });
  editForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
    ...this.detailsControls
  });
  constructor(private fb: FormBuilder, private instructors: InstructorService, private auth: AuthService,
    private router: Router,
    private otp: OtpService, private route: ActivatedRoute) { }
  ngOnInit(): void {
    if (this.auth.isLoggedIn()) {
      const role = this.auth.getRole();
      if (role === INSTRUCTOR_ROLE) { this.router.navigate(['/instructor']); return; }
      this.otherRole = String(role || '').toLowerCase();
      this.mode = 'other-role';
      return;
    }
    this.mode = 'ready';
    // only to show a short note and the step list; failing here changes nothing
    this.otp.status().subscribe({
      next: s => (this.otpRequired = !!s.required), error: () =>
        (this.otpRequired = null)
    });
    // the login page links here with ?tab=check&q=<e-mail>
    const q = this.route.snapshot.queryParamMap;
    if (q.get('tab') === 'check') {
      this.tab = 'check';
      const id = (q.get('q') || '').trim();
      if (id) { this.checkForm.patchValue({ identifier: id }); this.lookup(); }
    }
  }
  ngOnDestroy(): void { clearInterval(this.timer); clearInterval(this.checkTimer); }
  get f() { return this.registerForm.controls; }
  get e() { return this.editForm.controls; }
  get id() { return this.checkForm.get('identifier'); }
  setTab(tab: Tab): void {
    this.tab = tab;
    this.errorMessage = '';
  }
  // ------------------------------------------------------------------ registration
  submit(): void {
    if (this.registerForm.invalid || this.loading) { this.registerForm.markAllAsTouched(); return; }
    this.loading = true;
    this.errorMessage = '';
    const { confirmPassword, ...body } = this.registerForm.value as any;
    body.profileLink = (body.profileLink || '').trim();
    this.instructors.register(body).subscribe({
      next: () => this.afterRegister(body.email, body.mobileNumber),
      error: err => {
        this.loading = false;
        this.errorMessage = err.status === 409 ? 'A user with this email already exists'
          : err.status === 429 ? 'Too many attempts. Please wait a while and try again.'
            : err.status === 400 ? (err.error?.message || 'Please check the entered details')
              : 'Registration failed. Please try again later.';
      }
    });
  }
  /** Application saved. If the server asks for OTP, stay on this page and show step 2; otherwise show the
  "submitted" card. */
  private afterRegister(email: string, mobile: string): void {
    this.email = email;
    this.otp.status().subscribe({
      next: s => {
        this.loading = false;
        this.otpRequired = !!s.required;
        if (!s.required) { this.regStep = 'done'; return; }
        this.maskedMobile = '******' + String(mobile).slice(-4);
        this.regStep = 'verify';
        this.sendCode();
      },
      error: () => { this.loading = false; this.regStep = 'done'; }
    });
  }
  /** From the "submitted" card: open the second tab and check this application. */
  checkMine(): void {
    this.tab = 'check';
    this.checkForm.patchValue({ identifier: this.email });
    this.lookup();
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
      next: () => { this.verifying = false; clearInterval(this.timer); this.regStep = 'done'; },
      error: err => {
        this.verifying = false;
        this.code = '';
        this.otpError = [400, 429].includes(err.status) ? (err.error?.message || 'That code did not work.')
          : 'Could not check the code. Please try again.';
      }
    });
  }
  // ------------------------------------------------------------------ "Already applied?"
  lookup(): void {
    if (this.checkForm.invalid || this.checking || this.checkWait > 0) {
      this.checkForm.markAllAsTouched();
      return;
    }
    const identifier = String(this.checkForm.value.identifier).trim();
    this.checking = true;
    this.checkError = '';
    this.notFound = false;
    this.editing = false;
    this.resubmitted = false;
    this.instructors.status(identifier).subscribe({
      next: r => { this.checking = false; this.result = r; this.lookedUp = identifier; },
      error: err => {
        this.checking = false;
        this.result = null;
        this.notFound = err.status === 404;
        this.checkError = [400, 404, 429].includes(err.status) ? (err.error?.message || 'Could not check the application.')
: 'Could not check the application. Please try again in a moment.';
        if (err.status === 429) { this.startCheckWait(Number(err.headers?.get('Retry-After')) || 60); }
      }
    });
  }
  /** The e-mail typed in the check box, when it was one (fixing an application needs it). */
  get lookedUpEmail(): string {
    return this.lookedUp.includes('@') ? this.lookedUp.toLowerCase() : '';
  }
  /** The vertical progress list. */
  get steps(): TrackerStep[] {
    const r = this.result;
    if (!r) { return []; }
    const out: TrackerStep[] = [{
      title: 'Application submitted', detail: r.submittedAt ?
        this.when(r.submittedAt) : 'Received', state: 'done', icon: 'check'
    }];
    const mobileOk = !r.mobileCheckOn || r.mobileVerified;
    if (r.mobileCheckOn) {
      out.push({
        title: 'Mobile number verified', detail: r.mobileVerified ? 'Your number is verified' :
          'Verify your number with the code we text you',
        state: r.mobileVerified ? 'done' : 'now', icon: r.mobileVerified ? 'check' : 'smartphone'
      });
    }
    const reviewed = r.status !== 'PENDING';
    out.push({
      title: 'Reviewed by an administrator', detail: reviewed ? (r.reviewedAt ?
        this.when(r.reviewedAt) : 'Reviewed') : 'An administrator is checking your details',
      state: reviewed ? 'done' : (mobileOk ? 'now' : 'todo'), icon: reviewed ? 'check' : 'hourglass_top'
    });
    out.push({
      title: r.status === 'APPROVED' ? 'Approved' : r.status === 'REJECTED' ? 'Not approved' :
        'Decision',
      detail: r.status === 'APPROVED' ? 'Your application was accepted' : r.status === 'REJECTED' ? 'See the reason below' : 'You will see it here', state: r.status === 'APPROVED' ? 'ok' : r.status === 'REJECTED' ? 'bad' : 'todo',
      icon: r.status === 'APPROVED' ? 'verified' : r.status === 'REJECTED' ? 'close' : 'flag'
    });
    out.push({
      title: 'Login access', detail: r.canLogin ? 'You can log in now' : 'Opens after approval' +
        (r.mobileCheckOn ? ' and a verified mobile number' : ''),
      state: r.canLogin ? 'ok' : 'todo', icon: r.canLogin ? 'lock_open' : 'lock'
    });
    return out;
  }
  private when(iso: string): string {
    const d = new Date(iso);
    return isNaN(d.getTime()) ? '' : d.toLocaleString('en-IN', {
      day: 'numeric', month: 'short', year:
        'numeric', hour: 'numeric', minute: '2-digit'
    });
  }
  // ------------------------------------------------------------------ fix and resubmit
  startEdit(): void {
    this.editForm.reset({
      email: this.lookedUpEmail, password: '', qualification: '', experienceYears:
        null, expertise: '', bio: '', profileLink: ''
    });
    this.editError = '';
    this.editing = true;
  }
  cancelEdit(): void {
    this.editing = false;
    this.editError = '';
  }
  resubmit(): void {
    if (this.editForm.invalid || this.saving) { this.editForm.markAllAsTouched(); return; }
    this.saving = true;
    this.editError = '';
    const v: any = this.editForm.value;
    this.instructors.resubmit({
      ...v, email: String(v.email).trim(), profileLink: (v.profileLink ||
        '').trim()
    }).subscribe({
      next: () => {
        this.saving = false;
        this.editing = false;
        this.checkForm.patchValue({ identifier: String(v.email).trim() });
        this.lookup(); // shows the fresh status (PENDING again)
        this.resubmitted = true; // lookup() clears it first, so it is set after
      },
      error: err => {
        this.saving = false;
        this.editError = err.status === 401 ? 'Wrong e-mail or password'
          : [400, 403, 429].includes(err.status) ? (err.error?.message || 'Please check the entered details')
: 'Could not save your changes. Please try again.';
      }
    });
  }
  // ------------------------------------------------------------------ timers
  private startCountdown(seconds: number): void {
    clearInterval(this.timer);
    this.secondsLeft = seconds;
    this.timer = setInterval(() => {
      this.secondsLeft = Math.max(0, this.secondsLeft - 1);
      if (this.secondsLeft === 0) { clearInterval(this.timer); }
    }, 1000);
  }
  private startCheckWait(seconds: number): void {
    clearInterval(this.checkTimer);
    this.checkWait = seconds;
    this.checkTimer = setInterval(() => {
      this.checkWait = Math.max(0, this.checkWait - 1);
      if (this.checkWait === 0) { clearInterval(this.checkTimer); }
    }, 1000);
  }
}
