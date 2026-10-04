/** Filters accepted by the course search. All optional. */
export interface CourseSearchParams {
  keyword?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  sort?: 'newest' | 'priceAsc' | 'priceDesc' | 'name';
  page?: number;
  size?: number;
}
