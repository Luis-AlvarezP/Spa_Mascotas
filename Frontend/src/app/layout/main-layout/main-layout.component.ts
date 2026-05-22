import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet, ActivatedRoute, Router, NavigationEnd } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { ConfirmModalComponent } from '../../shared/components/confirm-modal/confirm-modal.component';
import { PedidoToastComponent } from '../../shared/components/pedido-toast/pedido-toast.component';
import { StockToastComponent } from '../../shared/components/stock-toast/stock-toast.component';
import { CitaToastComponent } from '../../shared/components/cita-toast/cita-toast.component';
import { GroomingToastComponent } from '../../shared/components/grooming-toast/grooming-toast.component';
import { InsumoToastComponent } from '../../shared/components/insumo-toast/insumo-toast.component';
import { PedidoNotificacionService } from '../../core/services/pedido-notificacion.service';
import { StockNotificacionService } from '../../core/services/stock-notificacion.service';
import { CitaNotificacionService } from '../../core/services/cita-notificacion.service';
import { GroomingNotificacionService } from '../../core/services/grooming-notificacion.service';
import { InsumoNotificacionService } from '../../core/services/insumo-notificacion.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, SidebarComponent, TopbarComponent, ConfirmModalComponent, PedidoToastComponent, StockToastComponent, CitaToastComponent, GroomingToastComponent, InsumoToastComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
})
export class MainLayoutComponent implements OnInit {
  private router = inject(Router);
  private route  = inject(ActivatedRoute);
  private pedidoNotif   = inject(PedidoNotificacionService);
  private stockNotif    = inject(StockNotificacionService);
  private citaNotif     = inject(CitaNotificacionService);
  private groomingNotif = inject(GroomingNotificacionService);
  private insumoNotif   = inject(InsumoNotificacionService);

  sidebarOpen = signal(false);

  ngOnInit(): void {
    this.pedidoNotif.init();
    this.stockNotif.init();
    this.citaNotif.init();
    this.groomingNotif.init();
    this.insumoNotif.init();
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
