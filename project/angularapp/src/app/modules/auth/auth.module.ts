import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from '../../components/login/login.component';
import { RegistrationComponent } from '../../components/registration/registration.component';
import { ForgotPasswordComponent } from '../../components/forgot-password/forgot-password.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'registration', component: RegistrationComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', redirectTo: 'forgot-password' }   // old e-mailed links now land on the new page
];

/** Login, registration and forgot / reset password. Lazy loaded: nobody needs this code until they open the login page. @author Suriya */
@NgModule({
  declarations: [LoginComponent, RegistrationComponent, ForgotPasswordComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule {}
