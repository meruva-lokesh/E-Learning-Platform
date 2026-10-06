import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Course } from '../../models/course.model';
import { CourseRating } from '../../models/course-rating.model';

/**
 * One course tile, used on the admin course list and the customer catalog.
 * It only shows data and raises events; the page decides what happens.
 *
 * @author Amogh
 */
@Component({
  selector: 'app-course-card',
  templateUrl: './course-card.component.html',
  styleUrls: ['./course-card.component.css']
})
export class CourseCardComponent {
  @Input() course!: Course;
  @Input() rating?: CourseRating;
  /** 'customer' shows Add to Cart, 'admin' shows Edit and Delete. */
  @Input() mode: 'admin' | 'customer' = 'customer';
  @Input() inCart = false;
  /** True when the customer already bought this course: the tile shows "Go to course" instead of Add to Cart. */
  @Input() enrolled = false;
  @Output() addToCart = new EventEmitter<Course>();
  @Output() edit = new EventEmitter<Course>();
  @Output() remove = new EventEmitter<Course>();
}
