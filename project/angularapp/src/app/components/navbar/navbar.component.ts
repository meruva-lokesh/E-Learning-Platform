import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  role: string | null = null;
  private sub?: Subscription;

  constructor(private auth: AuthService, private router: Router, private dialog: MatDialog) {}

  ngOnInit(): void {
    this.sub = this.auth.role$.subscribe(r => (this.role = r));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  askLogout(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Log out?',
        message: 'Are you sure you want to logout?',
        confirmText: 'Yes, Logout',
        cancelText: 'Cancel',
        icon: 'logout'
      }
    }).afterClosed().subscribe(yes => {
      if (yes) {
        this.auth.logout();
        this.router.navigate(['/login']);
      }
    });
  }
}
