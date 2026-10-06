import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { AuthGuard } from './components/authguard/authguard.guard';
import { HomeComponent } from './components/home/home.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { ErrorComponent } from './components/error/error.component';
import { ROLES } from './constant';

const ADMIN = { roles: [ROLES.ADMIN] };
const CUSTOMER = { roles: [ROLES.CUSTOMER] };
const ANY = { roles: [ROLES.ADMIN, ROLES.CUSTOMER] };

/**
 * The home page is the first page. Feature areas are lazy loaded (their code is downloaded only when needed)
 * and every protected area sits behind AuthGuard with the roles allowed in it.
 *
 * @author Team Lead
 */
const routes: Routes = [
  { path: '', component: HomeComponent, pathMatch: 'full' },                                       // the home page is the first page
  { path: 'home', redirectTo: '', pathMatch: 'full' },
  { path: '', loadChildren: () => import('./modules/auth/auth.module').then(m => m.AuthModule) },   // login, registration
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard], data: ANY },

  // admin pages
  { path: '', canActivate: [AuthGuard], data: ADMIN, loadChildren: () => import('./modules/admin/admin.module').then(m => m.AdminModule) },
  // customer pages
  { path: '', canActivate: [AuthGuard], data: CUSTOMER, loadChildren: () => import('./modules/customer/customer.module').then(m => m.CustomerModule) },
  // pages for both roles
  { path: '', canActivate: [AuthGuard], data: ANY, loadChildren: () => import('./modules/course-details/course-details.module').then(m => m.CourseDetailsModule) },

  { path: 'error', component: ErrorComponent },
  { path: '**', component: ErrorComponent }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
