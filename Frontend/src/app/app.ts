import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DOCUMENT } from '@angular/common';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private document    = inject(DOCUMENT);

  private readonly ACTIVITY_EVENTS = ['mousemove', 'keydown', 'click', 'touchstart', 'scroll'];
  private readonly onActivity = () => this.authService.resetInactividadTimer();

  ngOnInit(): void {
    this.ACTIVITY_EVENTS.forEach(ev =>
      this.document.addEventListener(ev, this.onActivity, { passive: true })
    );
    this.authService.resetInactividadTimer();
  }

  ngOnDestroy(): void {
    this.ACTIVITY_EVENTS.forEach(ev =>
      this.document.removeEventListener(ev, this.onActivity)
    );
  }
}
