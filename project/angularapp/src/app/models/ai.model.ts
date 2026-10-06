/** Shapes of the AI quiz and AI chatbot API (see AiAssistantController in the backend). */

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface QuizQuestionView {
  index: number;
  question: string;
  options: string[];
}

export interface QuizResponse {
  quizId: string;
  courseId: number;
  courseTitle: string;
  difficulty: Difficulty;
  questions: QuizQuestionView[];
}

export interface QuizResultItem {
  index: number;
  question: string;
  options: string[];
  yourAnswer: number | null;
  correctAnswer: number;
  correct: boolean;
  explanation: string;
}

export interface QuizResult {
  score: number;
  total: number;
  percent: number;
  items: QuizResultItem[];
}

export interface QuizHistoryItem {
  attemptId: number;
  courseId: number;
  courseTitle: string;
  difficulty: string;
  score: number;
  total: number;
  takenAt: string;
}

export interface ChatTurn {
  role: 'user' | 'bot';
  text: string;
}

export interface ChatCourse {
  courseId: number;
  courseType: string;
  coursePrice: number;
}

export interface ChatResponse {
  reply: string;
  courses: ChatCourse[];
  aiUsed: boolean;
}
