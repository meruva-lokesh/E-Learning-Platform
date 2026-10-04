import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * Error page. Shown for unknown URLs (404) and when a logged-in user opens a page that belongs to the other role (403).
 */
@Component({
  selector: 'app-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.css']
})
export class ErrorComponent {
  forbidden: boolean;
  loggedIn: boolean;

  constructor(route: ActivatedRoute, auth: AuthService) {
    this.forbidden = route.snapshot.queryParamMap.get('code') === '403';
    this.loggedIn = auth.isLoggedIn();
  }
}