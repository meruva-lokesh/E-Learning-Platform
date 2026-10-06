import { HttpClient, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { CourseVideo, CourseVideoStats, LearningItem, VideoProgress, VideoTicket } from '../models/video.model';

/** Lesson videos: upload, list, watch (ticket + progress) and the staff statistics. */
@Injectable({
  providedIn: 'root'
})
export class VideoService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  /** Returns the raw events so the page can draw an upload progress bar. */
  upload(courseId: number, form: FormData): Observable<HttpEvent<CourseVideo>> {
    return this.http.post<CourseVideo>(`${this.apiUrl}${API.VIDEO}/course/${courseId}`, form, { reportProgress: true, observe: 'events' });
  }

  list(courseId: number): Observable<CourseVideo[]> {
    return this.http.get<CourseVideo[]>(`${this.apiUrl}${API.VIDEO}/course/${courseId}`);
  }

  stats(courseId: number): Observable<CourseVideoStats> {
    return this.http.get<CourseVideoStats>(`${this.apiUrl}${API.VIDEO}/course/${courseId}/stats`);
  }

  myLearning(): Observable<LearningItem[]> {
    return this.http.get<LearningItem[]>(`${this.apiUrl}${API.VIDEO}/my-learning`);
  }

  ticket(videoId: number): Observable<VideoTicket> {
    return this.http.get<VideoTicket>(`${this.apiUrl}${API.VIDEO}/${videoId}/ticket`);
  }

  saveProgress(videoId: number, positionSec: number, durationSec: number): Observable<VideoProgress> {
    return this.http.put<VideoProgress>(`${this.apiUrl}${API.VIDEO}/${videoId}/progress`, { positionSec, durationSec });
  }

  delete(videoId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}${API.VIDEO}/${videoId}`);
  }

  /** The player's address: the ticket URL from the server, placed after the API address. */
  streamUrl(ticket: VideoTicket): string {
    return `${this.apiUrl}${ticket.url}`;
  }
}
