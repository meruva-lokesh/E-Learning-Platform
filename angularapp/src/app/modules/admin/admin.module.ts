import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AddCourseComponent } from '../../components/add-course/add-course.component';
import { EditCourseComponent } from '../../components/edit-course/edit-course.component';
import { ViewCoursesComponent } from '../../components/view-courses/view-courses.component';
import { ViewOrdersComponent } from '../../components/view-orders/view-orders.component';
import { ViewReviewComponent } from '../../components/view-review/view-review.component';
import { ConfirmDialogComponent } from 'src/app/components/confirm-dialog/confirm-dialog.component';
import { ManageAdminsComponent } from 'src/app/manage-admins/manage-admins/manage-admins.component';

const routes: Routes = [
  { path: 'add-course', component: AddCourseComponent },
  { path: 'edit-course/:id', component: EditCourseComponent },
  { path: 'view-courses', component: ViewCoursesComponent },
  { path: 'view-orders', component: ViewOrdersComponent },
  { path: 'view-review', component: ViewReviewComponent },
  { path: 'manage-admins', component: ManageAdminsComponent }
];

/** Pages only an admin can open. Lazy loaded and protected by AuthGuard (role ADMIN) in app-routing. @author Sivamuthu */
@NgModule({
  declarations: [AddCourseComponent, EditCourseComponent, ViewCoursesComponent, ViewOrdersComponent, ViewReviewComponent, ManageAdminsComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {}