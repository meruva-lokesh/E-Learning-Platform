import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CustomerService } from '../../services/customer.service';
import { UserStoreService } from '../../services/user-store.service';
import { AdminStats, CustomerStats } from '../../models/stats.model';
import { ROLES, STORAGE } from '../../constant';

/**
 * Landing page after login: a welcome band and the numbers from the dashboard statistics API.
 *
 * @author Meruva Lokesh
 */
@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  role: string | null;
  name = '';
  customerStats?: CustomerStats;
  adminStats?: AdminStats;
  statsFailed = false;

  constructor(private auth: AuthService, private customerService: CustomerService, private userStore: UserStoreService, private router: Router) {
    this.role = this.auth.getRole();
  }

  ngOnInit(): void {
    this.name = this.userStore.user?.username || '';
    if (this.role === ROLES.ADMIN) {
      this.customerService.getAdminStats().subscribe({
        next: (s) => (this.adminStats = s),
        error: () => (this.statsFailed = true)
      });
    } else if (this.role === ROLES.CUSTOMER) {
      const id = localStorage.getItem(STORAGE.CUSTOMER_ID);
      const load = (customerId: any) => this.customerService.getCustomerStats(customerId).subscribe({
        next: (s) => (this.customerStats = s),
        error: () => (this.statsFailed = true)
      });
      if (id) { load(id); } else {
        this.customerService.viewCustomerByUserId().subscribe({
          next: (c) => load(c.customerId),
          // no customer record yet: the "Fill Your Details" page comes first
          error: (err) => err.status === 404 ? this.router.navigate(['/customerdashboard']) : (this.statsFailed = true)
        });
      }
    }
  }
}
