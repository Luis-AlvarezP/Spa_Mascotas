import { Component, EventEmitter, Input, Output, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon:  string;
  route: string;
  roles: string[];
}

const ALL_NAV: NavItem[] = [
  { label: 'Inicio',              icon: 'home',     route: '/dashboard', roles: ['ADMIN','RECEPCION','GROOMER','CLIENTE'] },
  { label: 'Horarios de Trabajo', icon: 'calendar', route: '/agenda',    roles: ['ADMIN','RECEPCION'] },
  { label: 'Agenda',              icon: 'calendar', route: '/agenda',    roles: ['GROOMER'] },
  { label: 'Mis Citas',           icon: 'calendar', route: '/agenda',    roles: ['CLIENTE'] },
  { label: 'Grooming',            icon: 'scissors', route: '/grooming',  roles: ['ADMIN','GROOMER','CLIENTE'] },
  { label: 'Clientes y Mascotas', icon: 'pets',     route: '/mascotas',  roles: ['ADMIN','RECEPCION','GROOMER'] },
  { label: 'Mis Mascotas',        icon: 'paw',      route: '/mascotas',  roles: ['CLIENTE'] },
  { label: 'Inventario',          icon: 'box',      route: '/inventario',roles: ['ADMIN','RECEPCION','GROOMER','CLIENTE'] },
  { label: 'Administración',      icon: 'cog',      route: '/admin',     roles: ['ADMIN'] },
];

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  @Input()  open   = false;
  @Output() closed = new EventEmitter<void>();

  auth      = inject(AuthService);
  sanitizer = inject(DomSanitizer);

  navItems = computed(() => {
    const rol = this.auth.rol() ?? '';
    return ALL_NAV.filter(i => i.roles.includes(rol));
  });

  rolBadgeClass = computed(() => `badge-${(this.auth.rol() ?? 'cliente').toLowerCase()}`);

  close() { this.closed.emit(); }

  icon(name: string): SafeHtml {
    const m: Record<string, string> = {
      home:     `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z"/></svg>`,
      calendar: `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"/></svg>`,
      scissors: `<svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M5.5 2a3.5 3.5 0 101.665 6.58L8.585 10l-1.42 1.42a3.5 3.5 0 101.414 1.414L10 11.415l1.42 1.42a3.5 3.5 0 101.415-1.415L11.414 10l1.42-1.42A3.5 3.5 0 105 5.5a3.5 3.5 0 00.5 1.58zm1.5 0a1.5 1.5 0 110 3 1.5 1.5 0 010-3zm7 7a1.5 1.5 0 110 3 1.5 1.5 0 010-3zm-7 0a1.5 1.5 0 110 3 1.5 1.5 0 010-3z" clip-rule="evenodd"/></svg>`,
      pets:     `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M9 6a3 3 0 11-6 0 3 3 0 016 0zM17 6a3 3 0 11-6 0 3 3 0 016 0zM12.93 17c.046-.327.07-.66.07-1a6.97 6.97 0 00-1.5-4.33A5 5 0 0119 16v1h-6.07zM6 11a5 5 0 015 5v1H1v-1a5 5 0 015-5z"/></svg>`,
      paw:      `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M4.5 11a2.5 2.5 0 100-5 2.5 2.5 0 000 5zm15 0a2.5 2.5 0 100-5 2.5 2.5 0 000 5zM7 6.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5zm10 0a2.5 2.5 0 100-5 2.5 2.5 0 000 5zM12 13c-3.5 0-6 2.5-6 5.5 0 1.5 2 2.5 6 2.5s6-1 6-2.5c0-3-2.5-5.5-6-5.5z"/></svg>`,
      shop:     `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M3 1a1 1 0 000 2h1.22l.305 1.222a.997.997 0 00.01.042l1.358 5.43-.893.892C3.74 11.846 4.632 14 6.414 14H15a1 1 0 000-2H6.414l1-1H14a1 1 0 00.894-.553l3-6A1 1 0 0017 3H6.28l-.31-1.243A1 1 0 005 1H3z"/><path d="M16 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM6.5 18a1.5 1.5 0 100-3 1.5 1.5 0 000 3z"/></svg>`,
      box:      `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M4 3a2 2 0 100 4h12a2 2 0 100-4H4z"/><path fill-rule="evenodd" d="M3 8h14v7a2 2 0 01-2 2H5a2 2 0 01-2-2V8zm5 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" clip-rule="evenodd"/></svg>`,
      credit:   `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M4 4a2 2 0 00-2 2v1h16V6a2 2 0 00-2-2H4z"/><path fill-rule="evenodd" d="M18 9H2v5a2 2 0 002 2h12a2 2 0 002-2V9zM4 13a1 1 0 011-1h1a1 1 0 110 2H5a1 1 0 01-1-1zm5-1a1 1 0 100 2h1a1 1 0 100-2H9z" clip-rule="evenodd"/></svg>`,
      chart:    `<svg viewBox="0 0 20 20" fill="currentColor"><path d="M2 11a1 1 0 011-1h2a1 1 0 011 1v5a1 1 0 01-1 1H3a1 1 0 01-1-1v-5zm6-4a1 1 0 011-1h2a1 1 0 011 1v9a1 1 0 01-1 1H9a1 1 0 01-1-1V7zm6-3a1 1 0 011-1h2a1 1 0 011 1v12a1 1 0 01-1 1h-2a1 1 0 01-1-1V4z"/></svg>`,
      cog:      `<svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M11.49 3.17c-.38-1.56-2.6-1.56-2.98 0a1.532 1.532 0 01-2.286.948c-1.372-.836-2.942.734-2.106 2.106.54.886.061 2.042-.947 2.287-1.561.379-1.561 2.6 0 2.978a1.532 1.532 0 01.947 2.287c-.836 1.372.734 2.942 2.106 2.106a1.532 1.532 0 012.287.947c.379 1.561 2.6 1.561 2.978 0a1.533 1.533 0 012.287-.947c1.372.836 2.942-.734 2.106-2.106a1.533 1.533 0 01.947-2.287c1.561-.379 1.561-2.6 0-2.978a1.532 1.532 0 01-.947-2.287c.836-1.372-.734-2.942-2.106-2.106a1.532 1.532 0 01-2.287-.947zM10 13a3 3 0 100-6 3 3 0 000 6z" clip-rule="evenodd"/></svg>`,
    };
    return this.sanitizer.bypassSecurityTrustHtml(m[name] ?? m['home']);
  }
}
