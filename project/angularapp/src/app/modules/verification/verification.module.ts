import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { VerifyPhoneComponent } from '../../components/verify-phone/verify-phone.component';

const routes: Routes = [{ path: 'verify-phone', component: VerifyPhoneComponent }];

/**
 * The "Verify your mobile number" page (public). Import it in AppModule BEFORE AppRoutingModule so its route
 * is registered before the "**" fallback.
 */
@NgModule({
  declarations: [VerifyPhoneComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class VerificationModule {}
