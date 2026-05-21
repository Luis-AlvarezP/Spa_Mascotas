import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { GroomingNotificacionService } from '../../../core/services/grooming-notificacion.service';

@Component({
  selector: 'app-grooming-toast',
  standalone: true,
  templateUrl: './grooming-toast.component.html',
  styleUrl: './grooming-toast.component.scss',
})
export class GroomingToastComponent {
  notif  = inject(GroomingNotificacionService);
  router = inject(Router);

  irAGrooming(): void {
    this.notif.dismiss();
    this.router.navigate(['/grooming']);
  }
}
