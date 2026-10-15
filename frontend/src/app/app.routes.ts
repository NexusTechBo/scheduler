import { Routes } from '@angular/router';
import { authGuard, guestGuard, homeRedirect, roleGuard } from './core/auth/auth.guards';
import { ROLES } from './core/navigation/navigation';

/**
 * Every page inside the shell declares the roles allowed by the backend endpoints it uses.
 * Keep `core/navigation/navigation.ts` in sync when adding a page.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: homeRedirect },
      {
        path: 'dashboard',
        title: 'Inicio',
        canActivate: [roleGuard(...ROLES.staff)],
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'board',
        title: 'Tablero',
        canActivate: [roleGuard(...ROLES.staff)],
        loadComponent: () => import('./features/board/board.component').then((m) => m.BoardComponent)
      },
      {
        path: 'calendar',
        title: 'Calendario',
        loadComponent: () => import('./features/calendar/calendar.component').then((m) => m.CalendarComponent)
      },
      {
        path: 'appointments',
        title: 'Citas',
        loadComponent: () => import('./features/appointments/appointments-page.component').then((m) => m.AppointmentsPageComponent)
      },
      {
        path: 'appointments/new',
        title: 'Agendar cita',
        canActivate: [roleGuard(...ROLES.booking)],
        loadComponent: () => import('./features/appointments/booking/booking-page.component').then((m) => m.BookingPageComponent)
      },
      {
        path: 'patients',
        title: 'Pacientes',
        canActivate: [roleGuard(...ROLES.clinical)],
        loadComponent: () => import('./features/patients/patients-page.component').then((m) => m.PatientsPageComponent)
      },
      {
        path: 'availability',
        title: 'Mi disponibilidad',
        canActivate: [roleGuard(...ROLES.doctor)],
        loadComponent: () => import('./features/availability/availability-page.component').then((m) => m.AvailabilityPageComponent)
      },
      {
        path: 'staff',
        title: 'Personal',
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () => import('./features/staff/staff-page.component').then((m) => m.StaffPageComponent)
      },
      {
        path: 'specialties',
        title: 'Especialidades',
        canActivate: [roleGuard(...ROLES.admin)],
        loadComponent: () => import('./features/specialties/specialties-page.component').then((m) => m.SpecialtiesPageComponent)
      },
      {
        path: 'assistant',
        title: 'Asistente IA',
        canActivate: [roleGuard(...ROLES.patient)],
        loadComponent: () => import('./features/assistant/assistant-page.component').then((m) => m.AssistantPageComponent)
      },
      {
        path: 'profile',
        title: 'Mi perfil',
        loadComponent: () => import('./features/profile/profile-page.component').then((m) => m.ProfilePageComponent)
      }
    ]
  },
  {
    path: 'clinic-options',
    title: 'Selecciona tu clínica',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/clinic-options/clinic-options.component').then((m) => m.ClinicOptionsComponent)
  },
  {
    path: 'login',
    title: 'Iniciar sesión',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    title: 'Registrar clínica',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/register/register.component').then((m) => m.RegisterComponent)
  },
  { path: '**', redirectTo: '' }
];
