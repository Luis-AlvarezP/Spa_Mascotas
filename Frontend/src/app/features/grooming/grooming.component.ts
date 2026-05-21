import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-grooming',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './grooming.component.html',
  styleUrl: './grooming.component.scss',
})
export class GroomingComponent {
  auth      = inject(AuthService);
  isCliente = computed(() => this.auth.rol() === 'CLIENTE');
}
