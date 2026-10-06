import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { Course } from '../models/course.model';
import { CourseSearchParams } from '../models/search-params.model';
import { PageResponse } from '../models/page-response.model';

/**
 * Course API calls. The token is added by the AuthInterceptor, never here.
 *
 * @author Sivamuthu (add, edit, delete), Amogh (view, search, filter, pagination)
 */
@Injectable({
  providedIn: 'root'
})
export class CourseService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  addCourse(courseData: Course): Observable<Course> {
    return this.http.post<Course>(`${this.apiUrl}${API.COURSE}`, courseData);
  }

  viewAllCourses(): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.apiUrl}${API.COURSE}`);
  }

  updateCourse(courseId: string, updatedCourse: Course): Observable<Course> {
    return this.http.put<Course>(`${this.apiUrl}${API.COURSE}/${courseId}`, updatedCourse);
  }

  deleteCourse(courseId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}${API.COURSE}/${courseId}`);
  }

  getCourseById(courseId: string): Observable<Course> {
    return this.http.get<Course>(`${this.apiUrl}${API.COURSE}/${courseId}`);
  }

  /** Search, filter, sort and page the catalog on the server. Empty filters are not sent. */
  searchCourses(params: CourseSearchParams): Observable<PageResponse<Course>> {
    let query = new HttpParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        query = query.set(key, String(value));
      }
    });
    return this.http.get<PageResponse<Course>>(`${this.apiUrl}${API.COURSE_SEARCH}`, { params: query });
  }
}
