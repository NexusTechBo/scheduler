import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { ROLES } from './core/auth/roles';
import { GUEST_BOOKING_PATH, PAGE_TITLES } from './core/navigation/navigation';

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
        title: PAGE_TITLES.dashboard,
        loadComponent: () =>
          import('./features/dashboard/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
      {
        path: 'board',
        title: PAGE_TITLES.board,
        loadComponent: () =>
          import('./features/board/board-page.component').then((m) => m.BoardPageComponent),
      },
      {
        path: 'calendar',
        title: PAGE_TITLES.calendar,
        loadComponent: () =>
          import('./features/calendar/calendar-page.component').then(
            (m) => m.CalendarPageComponent,
          ),
      },
      {
        path: 'appointments',
        title: PAGE_TITLES.appointments,
        loadComponent: () =>
          import('./features/appointments/appointments-page.component').then(
            (m) => m.AppointmentsPageComponent,
          ),
      },
      {
        path: 'appointments/new',
        title: PAGE_TITLES.booking,
        loadComponent: () =>
          import('./features/booking/booking-page.component').then((m) => m.BookingPageComponent),
      },
      {
        path: 'patients',
        title: PAGE_TITLES.patients,
        loadComponent: () =>
          import('./features/patients/patients-page.component').then(
            (m) => m.PatientsPageComponent,
          ),
      },
      {
        path: 'availability',
        title: PAGE_TITLES.availability,
        canActivate: [roleGuard(...ROLES.doctor)],
        loadComponent: () =>
          import('./features/availability/availability-page.component').then(
            (m) => m.AvailabilityPageComponent,
          ),
      },
      {
        path: 'staff',
        title: PAGE_TITLES.staff,
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () =>
          import('./features/staff/staff-page.component').then((m) => m.StaffPageComponent),
      },
      {
        path: 'specialties',
        title: PAGE_TITLES.specialties,
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () =>
          import('./features/specialties/specialties-page.component').then(
            (m) => m.SpecialtiesPageComponent,
          ),
      },
      {
        path: 'profile',
        title: PAGE_TITLES.profile,
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
