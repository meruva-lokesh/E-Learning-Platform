import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../components/authguard/authguard.guard';
import { ROLES } from '../../constant';
import { AiChatbotComponent } from '../../components/ai-chatbot/ai-chatbot.component';
import { AiQuizComponent } from '../../components/ai-quiz/ai-quiz.component';

const routes: Routes = [
  { path: 'ai-quiz', component: AiQuizComponent, canActivate: [AuthGuard], data: { roles: [ROLES.CUSTOMER] } }
];

/**
 * AI quiz page and the floating AI chatbot. The chatbot is exported so AppComponent can show it on every page;
 * it only appears for a logged-in customer. This module must be imported in AppModule BEFORE AppRoutingModule,
 * so its route is registered before the "**" fallback route.
 */
@NgModule({
  declarations: [AiChatbotComponent, AiQuizComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
  exports: [AiChatbotComponent]
})
export class AiModule {}
