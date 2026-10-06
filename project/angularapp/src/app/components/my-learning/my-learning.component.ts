import { Component, OnInit } from '@angular/core';
import { LearningItem } from '../../models/video.model';
import { VideoService } from '../../services/video.service';

/** A customer's courses that have videos, with how much is done and a button to continue. */
@Component({
  selector: 'app-my-learning',
  templateUrl: './my-learning.component.html',
  styleUrls: ['./my-learning.component.css']
})
export class MyLearningComponent implements OnInit {
  items: LearningItem[] = [];
  loaded = false;
  loadError = '';

  constructor(private api: VideoService) {}

  ngOnInit(): void {
    this.api.myLearning().subscribe({
      next: list => { this.items = list; this.loaded = true; },
      error: err => {
        this.loaded = true;
        this.loadError = err.status === 404 ? 'Complete your profile first, then your lessons will appear here.' : 'Could not load your lessons. Please try again.';
      }
    });
  }

  trackById(_: number, i: LearningItem): number { return i.courseId; }
}
