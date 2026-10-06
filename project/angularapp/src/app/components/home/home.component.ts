import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  /** null for visitors who are not logged in. */
  role: string | null;

  constructor(auth: AuthService) {
    this.role = auth.isLoggedIn() ? auth.getRole() : null;
  }
}
