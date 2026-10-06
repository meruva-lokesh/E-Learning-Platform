import { Component, ElementRef, HostListener, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Subscription } from 'rxjs';
import { AiAssistantService } from '../../services/ai-assistant.service';
import { AuthService } from '../../services/auth.service';
import { ChatCourse, ChatTurn } from '../../models/ai.model';

interface Bubble {
  from: 'user' | 'bot';
  text: string;
  courses?: ChatCourse[];
}

/**
 * Floating AI assistant for customers. The conversation lives only in this component (no database):
 * every message sends the last few turns so the AI knows what was said before.
 */
@Component({
  selector: 'app-ai-chatbot',
  templateUrl: './ai-chatbot.component.html',
  styleUrls: ['./ai-chatbot.component.css']
})
export class AiChatbotComponent implements OnInit, OnDestroy {
  static readonly MAX_LENGTH = 500;
  static readonly HISTORY_TURNS = 8;

  role: string | null = null;
  open = false;
  draft = '';
  sending = false;
  error = '';
  bubbles: Bubble[] = [];
  readonly suggestions = ['Which course should I start with?', 'Find me a cheap beginner course', 'What have I enrolled in?'];
  readonly maxLength = AiChatbotComponent.MAX_LENGTH;

  @ViewChild('scroller') scroller?: ElementRef<HTMLElement>;
  @ViewChild('box') box?: ElementRef<HTMLTextAreaElement>;
  private sub?: Subscription;

  constructor(private ai: AiAssistantService, private auth: AuthService) {}

  ngOnInit(): void {
    this.sub = this.auth.role$.subscribe(r => {
      this.role = r;
      if (r !== 'CUSTOMER') { this.reset(); } // logged out or another user: forget the chat
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  toggle(): void {
    this.open = !this.open;
    if (this.open) { setTimeout(() => this.box?.nativeElement.focus(), 0); this.scrollDown(); }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) { this.open = false; }
  }

  reset(): void {
    this.open = false;
    this.bubbles = [];
    this.draft = '';
    this.error = '';
    this.sending = false;
  }

  clear(): void {
    this.bubbles = [];
    this.error = '';
  }

  useSuggestion(text: string): void {
    this.draft = text;
    this.send();
  }

  onEnter(ev: Event): void {
    if ((ev as KeyboardEvent).shiftKey) { return; }
    ev.preventDefault();
    this.send();
  }

  send(): void {
    const text = this.draft.trim();
    if (!text || this.sending || text.length > this.maxLength) { return; }
    const history: ChatTurn[] = this.bubbles
      .slice(-AiChatbotComponent.HISTORY_TURNS)
      .map(b => ({ role: b.from, text: b.text }));
    this.bubbles.push({ from: 'user', text });
    this.draft = '';
    this.error = '';
    this.sending = true;
    this.scrollDown();
    this.ai.chat(text, history).subscribe({
      next: res => {
        this.bubbles.push({ from: 'bot', text: res.reply, courses: res.courses });
        this.sending = false;
        this.scrollDown();
      },
      error: err => {
        this.sending = false;
        this.error = err.status === 429 || err.status === 400 || err.status === 503
          ? (err.error?.message || 'The assistant cannot answer right now.')
          : 'The assistant is unavailable. Please try again in a moment.';
        this.scrollDown();
      }
    });
  }

  trackByIndex(i: number): number {
    return i;
  }

  private scrollDown(): void {
    setTimeout(() => {
      const el = this.scroller?.nativeElement;
      if (el) { el.scrollTop = el.scrollHeight; }
    }, 0);
  }
}
