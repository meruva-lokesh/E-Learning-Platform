import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../components/authguard/authguard.guard';
import { ROLES } from '../../constant';
import { INSTRUCTOR_ROLE } from '../../models/instructor.model';
import { MyInvoicesComponent } from '../../components/my-invoices/my-invoices.component';
import { InvoiceViewComponent } from '../../components/invoice-view/invoice-view.component';
import { InstructorEarningsComponent } from '../../components/instructor-earnings/instructor-earnings.component';
import { AdminPayoutsComponent } from '../../components/admin-payouts/admin-payouts.component';

// 'invoice/order/:orderId' is listed before 'invoice/:id' so that "order" is never read as an id
const routes: Routes = [
  { path: 'my-invoices', component: MyInvoicesComponent, canActivate: [AuthGuard], data: { roles: [ROLES.CUSTOMER] } },
  { path: 'invoice/order/:orderId', component: InvoiceViewComponent, canActivate: [AuthGuard], data: { roles: [ROLES.CUSTOMER, ROLES.ADMIN] } },
  { path: 'invoice/:id', component: InvoiceViewComponent, canActivate: [AuthGuard], data: { roles: [ROLES.CUSTOMER, ROLES.ADMIN] } },
  { path: 'instructor/earnings', component: InstructorEarningsComponent, canActivate: [AuthGuard], data: { roles: [INSTRUCTOR_ROLE] } },
  { path: 'admin-payouts', component: AdminPayoutsComponent, canActivate: [AuthGuard], data: { roles: [ROLES.ADMIN] } }
];

/**
 * Invoices for customers, earnings for instructors, payouts for admins.
 * Import it in AppModule BEFORE AppRoutingModule so these routes are registered before the "**" fallback.
 */
@NgModule({
  declarations: [MyInvoicesComponent, InvoiceViewComponent, InstructorEarningsComponent, AdminPayoutsComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class BillingModule {}
