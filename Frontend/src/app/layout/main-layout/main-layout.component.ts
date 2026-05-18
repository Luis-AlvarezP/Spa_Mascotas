import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet, ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';
import { PedidoToastComponent } from '../../shared/components/pedido-toast/pedido-toast.component';
import { PedidoNotificacionService } from '../../core/services/pedido-notificacion.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, ConfirmModalComponent, PedidoToastComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent implements OnInit {
  private router = inject(Router);
  private route  = inject(ActivatedRoute);
  private pedidoNotif = inject(PedidoNotificacionService);

  sidebarOpen = signal(false);

  ngOnInit(): void {
    this.pedidoNotif.init();
  }

  title = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      startWith(null),
      map(() => {
        let r = this.route;
        while (r.firstChild) r = r.firstChild;
        return (r.snapshot?.data?.['title'] as string) ?? 'Panel de control';
      }),
    ),
    { initialValue: 'Panel de control' },
  );
}
