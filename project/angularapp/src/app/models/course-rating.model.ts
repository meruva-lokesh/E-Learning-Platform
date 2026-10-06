/** Average rating of one course, from GET /api/review/summary. */
export interface CourseRating {
  courseType?: string;
  averageRating?: number;
  reviewCount?: number;
}
