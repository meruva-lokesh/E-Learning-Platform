import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AiAssistantService } from '../../services/ai-assistant.service';
import { CourseService } from '../../services/course.service';
import { Course } from '../../models/course.model';
import { Difficulty, QuizHistoryItem, QuizResponse, QuizResult } from '../../models/ai.model';

type Step = 'setup' | 'loading' | 'taking' | 'submitting' | 'result';

/**
 * AI quiz page: pick a course, a difficulty and a number of questions, answer, then see the marks with
 * an explanation for every question. The correct answers only arrive after submitting (the server keeps them).
 */
@Component({
  selector: 'app-ai-quiz',
  templateUrl: './ai-quiz.component.html',
  styleUrls: ['./ai-quiz.component.css']
})
export class AiQuizComponent implements OnInit {
  step: Step = 'setup';
  courses: Course[] = [];
  coursesLoaded = false;
  aiEnabled = true;

  courseId: number | null = null;
  numQuestions = 5;
  difficulty: Difficulty = 'MEDIUM';
  readonly counts = [3, 5, 8, 10];
  readonly levels: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];

  quiz?: QuizResponse;
  answers: (number | null)[] = [];
  result?: QuizResult;
  history: QuizHistoryItem[] = [];
  error = '';

  constructor(private ai: AiAssistantService, private courseService: CourseService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const preset = Number(this.route.snapshot.queryParamMap.get('courseId'));
    if (preset > 0) { this.courseId = preset; }
    this.courseService.viewAllCourses().subscribe({
      next: list => { this.courses = list; this.coursesLoaded = true; },
      error: () => { this.coursesLoaded = true; this.error = 'Could not load the courses. Please refresh the page.'; }
    });
    this.ai.status().subscribe({ next: s => (this.aiEnabled = s.aiEnabled), error: () => {} });
    this.loadHistory();
  }

  get answered(): number {
    return this.answers.filter(a => a !== null).length;
  }

  get canStart(): boolean {
    return this.courseId !== null && this.step === 'setup' && this.aiEnabled;
  }

  get verdict(): string {
    const p = this.result?.percent ?? 0;
    return p >= 80 ? 'Great work!' : p >= 50 ? 'Good progress. A little more practice will help.' : 'Keep practising. Review the explanations below.';
  }

  start(): void {
    if (this.courseId === null) { return; }
    this.error = '';
    this.step = 'loading';
    this.ai.generateQuiz(this.courseId, this.numQuestions, this.difficulty).subscribe({
      next: q => {
        this.quiz = q;
        this.answers = q.questions.map(() => null);
        this.step = 'taking';
        window.scrollTo({ top: 0 });
      },
      error: err => { this.step = 'setup'; this.error = this.message(err, 'The quiz could not be created. Please try again.'); }
    });
  }

  pick(question: number, option: number): void {
    this.answers[question] = option;
  }

  submit(): void {
    if (!this.quiz || this.step !== 'taking') { return; }
    this.error = '';
    this.step = 'submitting';
    this.ai.submitQuiz(this.quiz.quizId, this.answers).subscribe({
      next: r => { this.result = r; this.step = 'result'; this.loadHistory(); window.scrollTo({ top: 0 }); },
      error: err => {
        // a quiz can be submitted once; if the server no longer knows it (expired), start again from the setup
        this.error = this.message(err, 'Your answers could not be submitted. Please try again.');
        this.step = err.status === 404 ? 'setup' : 'taking';
      }
    });
  }

  again(): void {
    this.step = 'setup';
    this.quiz = undefined;
    this.result = undefined;
    this.error = '';
  }

  letter(i: number): string {
    return String.fromCharCode(65 + i);
  }

  private loadHistory(): void {
    this.ai.quizHistory().subscribe({ next: h => (this.history = h), error: () => {} });
  }

  private message(err: any, fallback: string): string {
    return [400, 403, 404, 429, 502, 503].includes(err?.status) ? (err.error?.message || fallback) : fallback;
  }
}