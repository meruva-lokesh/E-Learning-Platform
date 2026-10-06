import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
import { AuthInterceptor } from './services/auth.interceptor';
import { AuthService } from './services/auth.service';

import { NavbarComponent } from './components/navbar/navbar.component';
import { HomeComponent } from './components/home/home.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { ErrorComponent } from './components/error/error.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { FooterComponent } from './components/footer/footer.component';
import { AiModule } from './modules/ai/ai.module';
import { VerificationModule } from './modules/verification/verification.module';
import { BillingModule } from './modules/billing/billing.module';
import { VideoModule } from './modules/video/video.module';
import { InstructorModule } from './modules/instructor/instructor.module';

/**
 * Only what every visitor needs is loaded up front: the navbar, home, dashboard, error page and the dialog.
 * Login, admin, customer and course-detail pages are lazy modules (see app-routing.module.ts).
 *
 * @author Team Lead
 */
@NgModule({
  declarations: [AppComponent, NavbarComponent, HomeComponent, DashboardComponent, ErrorComponent, ConfirmDialogComponent, FooterComponent],
  imports: [BrowserModule, BrowserAnimationsModule, HttpClientModule, SharedModule, AiModule, VerificationModule, InstructorModule,
    VideoModule,
    BillingModule,
    AppRoutingModule],
  providers: [
    // the token is added to API calls here and nowhere else (guideline 24)
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    // after a page reload, bring the session back (saved token or refresh cookie) before the first page renders
    {
      provide: APP_INITIALIZER,
      multi: true,
      deps: [AuthService],
      useFactory: (auth: AuthService) => () => firstValueFrom(auth.restoreSession())
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
