import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { Course } from '../models/course.model';

/**
 * Semantic ("AI") course search.
 *
 * @author Sumit
 */
@Injectable({
  providedIn: 'root'
})
export class AiService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  searchCourses(query: string): Observable<Course[]> {
    return this.http.post<Course[]>(`${this.apiUrl}${API.COURSE_AI_SEARCH}`, { query });
  }
}
