import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, FormGroupDirective, ValidationErrors, Validators } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from 'src/app/components/confirm-dialog/confirm-dialog.component';
import { User } from '../../models/user.model';
import { AdminService } from '../../services/admin.service';
import { UserStoreService } from '../../services/user-store.service';

function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}

/**
 * Admin-only page: see the admins, add a new one, remove one (never yourself, never the last).
 *
 * @author Suriya
 */
@Component({
  selector: 'app-manage-admins',
  templateUrl: './manage-admins.component.html',
  styleUrls: ['./manage-admins.component.css']
})
export class ManageAdminsComponent implements OnInit {
  admins: User[] = [];
  mainAdmin = false;          // only the main admin sees the Add and Remove controls
  pageIndex = 0;
  pageSize = 5;
  form: FormGroup;
  myEmail = '';
  errorMessage = '';
  loaded = false;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private adminService: AdminService,
    private userStore: UserStoreService,
    private dialog: MatDialog,
    private snack: MatSnackBar
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      mobileNumber: ['', [Validators.required, Validators.pattern(/^[0-9]{10}$/)]],
      password: ['', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: passwordsMatch });
  }

  get f() { return this.form.controls; }

  ngOnInit(): void {
    this.myEmail = (this.userStore.user?.email || '').toLowerCase();
    this.adminService.whoAmI().subscribe({ next: (me) => (this.mainAdmin = me.mainAdmin), error: () => (this.mainAdmin = false) });
    this.load();
  }

  /** The admins on the current page. */
  get pageItems(): User[] {
    const start = this.pageIndex * this.pageSize;
    return this.admins.slice(start, start + this.pageSize);
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  load(): void {
    this.adminService.listAdmins().subscribe({
      next: (list) => { this.admins = list; this.loaded = true; },
      error: () => { this.errorMessage = 'Could not load the admins.'; this.loaded = true; }
    });
  }

  isMe(admin: User): boolean {
    return (admin.email || '').toLowerCase() === this.myEmail;
  }

  add(formDirective: FormGroupDirective): void {
    if (this.form.invalid || this.saving) { return; }
    this.saving = true;
    this.errorMessage = '';
    const { confirmPassword, ...admin } = this.form.value;
    this.adminService.createAdmin({ ...admin, role: 'ADMIN' }).subscribe({
      next: () => {
        this.saving = false;
        formDirective.resetForm();   // clears the values and the red error state
        this.snack.open('Admin added', 'OK', { duration: 3000 });
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.status === 409 ? 'A user with this email already exists'
          : err.status === 400 ? (err.error?.message || 'Please check the entered details')
          : 'Could not add the admin. Please try again.';
      }
    });
  }

  remove(admin: User): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Remove admin?',
        message: `Remove ${admin.username} (${admin.email}) as an admin? They will no longer be able to log in.`,
        confirmText: 'Remove', cancelText: 'Cancel', icon: 'warning', danger: true
      }
    }).afterClosed().subscribe(yes => {
      if (!yes || admin.userId === undefined) { return; }
      this.adminService.deleteAdmin(admin.userId).subscribe({
        next: () => {
          this.snack.open('Admin removed', 'OK', { duration: 3000 });
          if (this.pageItems.length === 1 && this.pageIndex > 0) { this.pageIndex--; }   // last item of the page was removed
          this.load();
        },
        error: (err) => (this.errorMessage = err.error?.message || 'Could not remove the admin.')
      });
    });
  }
}