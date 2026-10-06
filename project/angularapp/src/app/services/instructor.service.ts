import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { Course } from '../models/course.model';
import {
  ApplicationStatusView, InstructorDetails, InstructorRegisterRequest, InstructorView,
  ResubmitRequest
} from '../models/instructor.model';
/** Instructor registration and application status, the admin's review, and an instructor's own courses. */
@Injectable({
  providedIn: 'root'
})
export class InstructorService {
  public apiUrl = API_URL;
  constructor(private http: HttpClient) { }
  // ----- the instructor
  register(body: InstructorRegisterRequest): Observable<InstructorView> {
    return this.http.post<InstructorView>(`${this.apiUrl}${API.INSTRUCTOR_REGISTER}`, body);
  }
  /** Public: where is my application? identifier = username or e-mail. */
  status(identifier: string): Observable<ApplicationStatusView> {
    return this.http.post<ApplicationStatusView>(`${this.apiUrl}${API.INSTRUCTOR_STATUS}`, { identifier });
  }
  /** Public: fix a pending or rejected application (instructors cannot log in until they are approved). */
  resubmit(body: ResubmitRequest): Observable<InstructorView> {
    return this.http.post<InstructorView>(`${this.apiUrl}${API.INSTRUCTOR_RESUBMIT}`, body);
  }
  me(): Observable<InstructorView> {
    return this.http.get<InstructorView>(`${this.apiUrl}${API.INSTRUCTOR_ME}`);
  }
  updateMe(details: InstructorDetails): Observable<InstructorView> {
    return this.http.put<InstructorView>(`${this.apiUrl}${API.INSTRUCTOR_ME}`, details);
  }
  myCourses(): Observable<Course[]> {
    return this.http.get<Course[]>(`${this.apiUrl}${API.INSTRUCTOR_COURSES}`);
  }
  addCourse(course: Course): Observable<Course> {
    return this.http.post<Course>(`${this.apiUrl}${API.INSTRUCTOR_COURSES}`, course);
  }
  updateCourse(courseId: number, course: Course): Observable<Course> {
    return this.http.put<Course>(`${this.apiUrl}${API.INSTRUCTOR_COURSES}/${courseId}`, course);
  }
  deleteCourse(courseId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}${API.INSTRUCTOR_COURSES}/${courseId}`);
  }
  // ----- the admin
  list(status: string = 'ALL'): Observable<InstructorView[]> {
    return this.http.get<InstructorView[]>(`${this.apiUrl}${API.ADMIN_INSTRUCTORS}`, {
      params: new
        HttpParams().set('status', status)
    });
  }
  approve(userId: number): Observable<InstructorView> {
    return this.http.post<InstructorView>(`${this.apiUrl}${API.ADMIN_INSTRUCTORS}/${userId}/approve`, {});
  }
  reject(userId: number, reason: string): Observable<InstructorView> {
    return this.http.post<InstructorView>(`${this.apiUrl}${API.ADMIN_INSTRUCTORS}/${userId}/reject`, {
      reason
    });
  }
}
