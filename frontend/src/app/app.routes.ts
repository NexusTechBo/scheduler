import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { HOME_URL, ROLES } from './core/navigation/navigation';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: HOME_URL },
      {
        path: 'dashboard',
        title: 'Inicio',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'board',
        title: 'Tablero',
        loadComponent: () =>
          import('./features/board/board.component').then((m) => m.BoardComponent),
      },
      {
        path: 'calendar',
        title: 'Calendario',
        loadComponent: () =>
          import('./features/calendar/calendar.component').then((m) => m.CalendarComponent),
      },
      {
        path: 'appointments',
        title: 'Citas',
        loadComponent: () =>
          import('./features/appointments/appointments-page.component').then(
            (m) => m.AppointmentsPageComponent,
          ),
      },
      {
        path: 'appointments/new',
        title: 'Agendar cita',
        loadComponent: () =>
          import('./features/booking/booking-page.component').then((m) => m.BookingPageComponent),
      },
      {
        path: 'patients',
        title: 'Pacientes',
        loadComponent: () =>
          import('./features/patients/patients-page.component').then(
            (m) => m.PatientsPageComponent,
          ),
      },
      {
        path: 'availability',
        title: 'Mi disponibilidad',
        canActivate: [roleGuard(...ROLES.doctor)],
        loadComponent: () =>
          import('./features/availability/availability-page.component').then(
            (m) => m.AvailabilityPageComponent,
          ),
      },
      {
        path: 'staff',
        title: 'Personal',
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () =>
          import('./features/staff/staff-page.component').then((m) => m.StaffPageComponent),
      },
      {
        path: 'specialties',
        title: 'Especialidades',
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () =>
          import('./features/specialties/specialties-page.component').then(
            (m) => m.SpecialtiesPageComponent,
          ),
      },
      {
        path: 'profile',
        title: 'Mi perfil',
        loadComponent: () =>
          import('./features/profile/profile-page.component').then((m) => m.ProfilePageComponent),
      },
    ],
  },
  {
    path: 'login',
    title: 'Iniciar sesión',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    title: 'Registrar clínica',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/register/register.component').then((m) => m.RegisterComponent),
  },
  { path: '**', redirectTo: '' },
];
