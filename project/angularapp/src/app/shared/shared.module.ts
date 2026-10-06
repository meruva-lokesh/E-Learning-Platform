import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MaterialModule } from '../material.module';
import { CourseCardComponent } from './course-card/course-card.component';
import { StarRatingComponent } from './star-rating/star-rating.component';

/**
 * Things every lazy-loaded feature module needs: Angular common pieces, forms, router links,
 * Angular Material and the small shared components.
 *
 * @author Team Lead
 */
@NgModule({
  declarations: [CourseCardComponent, StarRatingComponent],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, MaterialModule],
  exports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, MaterialModule, CourseCardComponent, StarRatingComponent]
})
export class SharedModule {}
