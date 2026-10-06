import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { CourseDetailsComponent } from '../../components/course-details/course-details.component';

const routes: Routes = [{ path: 'course-details/:id', component: CourseDetailsComponent }];

/** One course in full. Both admins and customers can open it. @author Amogh */
@NgModule({
  declarations: [CourseDetailsComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CourseDetailsModule {}
