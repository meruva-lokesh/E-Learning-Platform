import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { AuthGuard } from '../../components/authguard/authguard.guard';
import { ROLES } from '../../constant';
import { INSTRUCTOR_ROLE } from '../../models/instructor.model';
import { InstructorRegisterComponent } from '../../components/instructor-register/instructor-register.component';
import { AdminInstructorsComponent } from '../../components/admin-instructors/admin-instructors.component';
import { InstructorCoursesComponent } from 'src/app/components/instructor-courses/instructor-courses.component';
import { InstructorHomeComponent } from '../../components/instructor-home/instructor-home.component';
const routes: Routes = [
  // public: the registration form and the "Already applied?" status check
  { path: 'instructor-register', component: InstructorRegisterComponent },
  {
    path: 'admin-instructors', component: AdminInstructorsComponent, canActivate: [AuthGuard], data: {
      roles: [ROLES.ADMIN]
    }
  },
  {
    path: 'instructor', component: InstructorHomeComponent, canActivate: [AuthGuard], data: {
      roles:
        [INSTRUCTOR_ROLE]
    }
  },
  {
    path: 'instructor/courses', component: InstructorCoursesComponent, canActivate: [AuthGuard], data: {
      roles: [INSTRUCTOR_ROLE]
    }
  }
];
/**
* Everything for instructors: registration and the status check, the instructor home page, the admin's
approval page and the course CRUD.
* Import it in AppModule BEFORE AppRoutingModule so these routes are registered before the "**" fallback.
*/
@NgModule({
  declarations: [InstructorRegisterComponent, AdminInstructorsComponent, InstructorCoursesComponent,
    InstructorHomeComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class InstructorModule { }
