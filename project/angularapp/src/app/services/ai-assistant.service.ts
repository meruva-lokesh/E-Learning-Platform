import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { ChatResponse, ChatTurn, Difficulty, QuizHistoryItem, QuizResponse, QuizResult } from '../models/ai.model';

/**
 * AI quiz and AI chatbot calls. (The older AI course search stays in AiService.)
 * The login token is added by AuthInterceptor, so nothing about tokens appears here.
 */
@Injectable({
  providedIn: 'root'
})
export class AiAssistantService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  status(): Observable<{ aiEnabled: boolean }> {
    return this.http.get<{ aiEnabled: boolean }>(`${this.apiUrl}${API.AI_STATUS}`);
  }

  generateQuiz(courseId: number, numQuestions: number, difficulty: Difficulty): Observable<QuizResponse> {
    return this.http.post<QuizResponse>(`${this.apiUrl}${API.AI_QUIZ_GENERATE}`, { courseId, numQuestions, difficulty });
  }

  submitQuiz(quizId: string, answers: (number | null)[]): Observable<QuizResult> {
    return this.http.post<QuizResult>(`${this.apiUrl}${API.AI_QUIZ_SUBMIT}`, { quizId, answers });
  }

  quizHistory(): Observable<QuizHistoryItem[]> {
    return this.http.get<QuizHistoryItem[]>(`${this.apiUrl}${API.AI_QUIZ_HISTORY}`);
  }

  chat(message: string, history: ChatTurn[]): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiUrl}${API.AI_CHAT}`, { message, history });
  }
}
