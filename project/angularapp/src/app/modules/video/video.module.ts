import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../components/authguard/authguard.guard';
import { ROLES } from '../../constant';
import { INSTRUCTOR_ROLE } from '../../models/instructor.model';
import { ManageVideosComponent } from '../../components/manage-videos/manage-videos.component';
import { WatchVideoComponent } from '../../components/watch-video/watch-video.component';
import { MyLearningComponent } from '../../components/my-learning/my-learning.component';

const routes: Routes = [
  { path: 'manage-videos', component: ManageVideosComponent, canActivate: [AuthGuard], data: { roles: [ROLES.ADMIN, INSTRUCTOR_ROLE] } },
  { path: 'my-learning', component: MyLearningComponent, canActivate: [AuthGuard], data: { roles: [ROLES.CUSTOMER] } },
  { path: 'watch/:courseId', component: WatchVideoComponent, canActivate: [AuthGuard], data: { roles: [ROLES.ADMIN, ROLES.CUSTOMER, INSTRUCTOR_ROLE] } }
];

/**
 * Lesson videos for admins, instructors and customers.
 * Import it in AppModule BEFORE AppRoutingModule so these routes are registered before the "**" fallback.
 */
@NgModule({
  declarations: [ManageVideosComponent, WatchVideoComponent, MyLearningComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class VideoModule {}
