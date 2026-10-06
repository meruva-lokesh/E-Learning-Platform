/** Page of results returned by GET /api/course/search. */
export interface PageResponse<T> {
  content?: T[];
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}
