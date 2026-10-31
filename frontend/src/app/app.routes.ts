import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { GUEST_BOOKING_PATH, ROLES } from './core/navigation/navigation';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'ClinicFlow',
    loadComponent: () =>
      import('./features/landing/landing-page.component').then((m) => m.LandingPageComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        title: 'Inicio',
        loadComponent: () =>
          import('./features/dashboard/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
      {
        path: 'board',
        title: 'Tablero',
        loadComponent: () =>
          import('./features/board/board-page.component').then((m) => m.BoardPageComponent),
      },
      {
        path: 'calendar',
        title: 'Calendario',
        loadComponent: () =>
          import('./features/calendar/calendar-page.component').then(
            (m) => m.CalendarPageComponent,
          ),
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
  {
    path: 'activar',
    title: 'Activar clínica',
    loadComponent: () =>
      import('./features/activation/activation.component').then((m) => m.ActivationComponent),
  },
  {
    path: GUEST_BOOKING_PATH,
    title: 'Reservar cita',
    loadComponent: () =>
      import('./features/guest-booking/guest-booking.component').then(
        (m) => m.GuestBookingComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
