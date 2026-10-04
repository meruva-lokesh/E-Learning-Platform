import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from '../../components/login/login.component';
import { RegistrationComponent } from '../../components/registration/registration.component';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'registration', component: RegistrationComponent }
];

/** Login and registration. Lazy loaded: nobody needs this code until they open the login page. @author Suriya */
@NgModule({
  declarations: [LoginComponent, RegistrationComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule {}
