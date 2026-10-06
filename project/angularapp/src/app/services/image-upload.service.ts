import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { API, API_URL } from '../constant';

/**
 * Course image upload. The admin browses for a picture, it is sent to the server, and the server answers with the
 * public URL. That URL is saved in the course's existing courseImageUrl field, so the Course model does not change.
 */
@Injectable({
  providedIn: 'root'
})
export class ImageUploadService {
  public apiUrl = API_URL;

  /** Same limits as the server (the server checks again, this only gives a faster message). */
  readonly maxBytes = 5 * 1024 * 1024;
  readonly allowedTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];

  constructor(private http: HttpClient) {}

  /** Returns an error message, or null when the file can be uploaded. */
  validate(file: File): string | null {
    if (!this.allowedTypes.includes(file.type)) { return 'Choose a PNG, JPG, GIF or WEBP image'; }
    if (file.size > this.maxBytes) { return 'The image is too large. The limit is 5 MB'; }
    return null;
  }

  /** Sends the file and returns its public URL. The token is added by the AuthInterceptor. */
  upload(file: File): Observable<string> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<{ url: string }>(`${this.apiUrl}${API.UPLOAD_IMAGE}`, body).pipe(map(res => res.url));
  }
}
