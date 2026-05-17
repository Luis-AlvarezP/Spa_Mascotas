import { Routes } from '@angular/router';
import { authGuard, adminGuard, guestGuard } from './core/guards/auth.guard';


export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // ── Rutas públicas (sin layout) ──────────────────────────
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
      },
      {
        path: 'register',
        canActivate: [guestGuard],
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
      },
      {
        path: 'activar',
        loadComponent: () => import('./features/auth/activacion/activacion.component').then(m => m.ActivacionComponent),
      },
      {
        path: 'oauth-callback',
        loadComponent: () => import('./features/auth/oauth-callback/oauth-callback.component').then(m => m.OAuthCallbackComponent),
      },
      {
        path: 'setup-2fa',
        loadComponent: () => import('./features/auth/setup-2fa/setup-2fa.component').then(m => m.Setup2faComponent),
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
      },
      {
        path: 'reset-password',
        loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
      },
    ],
  },

  // ── Rutas protegidas (con MainLayout: sidebar + topbar) ──
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: 'dashboard',
        data: { title: 'Panel de control' },
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'perfil',
        data: { title: 'Mi perfil' },
        loadComponent: () => import('./features/perfil/perfil.component').then(m => m.PerfilComponent),
      },
      {
        path: 'agenda',
        data: { title: 'Agenda' },
        loadComponent: () => import('./features/agenda/agenda.component').then(m => m.AgendaComponent),
      },
      {
        path: 'grooming',
        data: { title: 'Grooming' },
        loadComponent: () => import('./features/grooming/grooming.component').then(m => m.GroomingComponent),
      },
      {
        path: 'mascotas',
        data: { title: 'Clientes y Mascotas' },
        loadComponent: () => import('./features/clientes/clientes.component').then(m => m.ClientesComponent),
      },
      {
        path: 'inventario',
        data: { title: 'Inventario' },
        loadComponent: () => import('./features/inventario/inventario.component').then(m => m.InventarioComponent),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          {
            path: 'auditoria',
            data: { title: 'Auditoría' },
            loadComponent: () => import('./features/admin/audit/audit.component').then(m => m.AuditComponent),
          },
          {
            path: 'usuarios',
            data: { title: 'Gestión de Personal' },
            loadComponent: () => import('./features/admin/usuarios/admin-usuarios.component').then(m => m.AdminUsuariosComponent),
          },
          { path: '', redirectTo: 'auditoria', pathMatch: 'full' },
        ],
      },
    ],
  },

  { path: '**', redirectTo: '/dashboard' },
];
