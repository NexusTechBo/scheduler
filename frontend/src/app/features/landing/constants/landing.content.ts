import {
  LandingFeature,
  LandingRole,
  LandingShot,
  LandingShowcase,
  LandingStep,
} from '../landing.model';

const SHOTS = 'landing/';

export const LANDING_NAV = [
  { label: 'Funciones', anchor: 'funciones' },
  { label: 'Producto', anchor: 'producto' },
  { label: 'Roles', anchor: 'roles' },
  { label: 'Cómo funciona', anchor: 'como-funciona' },
];

export const HERO_SHOT: LandingShot = {
  src: SHOTS + '11-assistant-board.webp',
  alt: 'Tablero de citas con columnas de pendientes, confirmadas y canceladas',
};

export const HERO_MOBILE_SHOT: LandingShot = {
  src: SHOTS + '41-mobile-assistant-dashboard.webp',
  alt: 'Inicio de ClinicFlow en un teléfono móvil',
};

export const LANDING_STATS = [
  { value: '3', label: 'roles con permisos propios' },
  { value: '24/7', label: 'agenda disponible en la nube' },
  { value: '100%', label: 'responsive, también en móvil' },
  { value: '0', label: 'instalaciones necesarias' },
];

export const LANDING_FEATURES: LandingFeature[] = [
  {
    icon: 'calendar',
    title: 'Agenda y calendario',
    description: 'Visualiza las citas del día, la semana o el mes de todos tus doctores.',
  },
  {
    icon: 'board',
    title: 'Tablero de citas',
    description: 'Confirma o cancela citas pendientes arrastrándolas entre columnas.',
  },
  {
    icon: 'clock',
    title: 'Disponibilidad por doctor',
    description: 'Cada doctor define su horario semanal y bloquea días u horas libres.',
  },
  {
    icon: 'patients',
    title: 'Gestión de pacientes',
    description: 'Registra pacientes y asígnalos a sus doctores en segundos.',
  },
  {
    icon: 'staff',
    title: 'Roles y personal',
    description: 'Administradores, doctores y asistentes, cada uno con sus permisos.',
  },
  {
    icon: 'alert',
    title: 'Notificaciones por email',
    description: 'Tus pacientes reciben un aviso al confirmar o cancelar su cita.',
  },
];

export const LANDING_SHOWCASE: LandingShowcase[] = [
  {
    eyebrow: 'Calendario',
    title: 'Todo el mes de un vistazo',
    description:
      'Cada día muestra cuántas citas tiene y su estado. Haz clic en un día para ver la agenda completa y agendar al instante.',
    points: [
      'Citas pendientes, confirmadas y canceladas por color',
      'Agenda del día al lado del calendario',
      'Agendar una cita desde el mismo día',
    ],
    shot: {
      src: SHOTS + '12-assistant-calendar.webp',
      alt: 'Calendario mensual con la agenda del día seleccionado',
    },
  },
  {
    eyebrow: 'Agendar cita',
    title: 'Reserva en tres pasos',
    description:
      'Elige al paciente, la especialidad y el doctor, y solo se muestran los horarios realmente libres. Sin dobles reservas.',
    points: [
      'Filtra doctores por especialidad',
      'Horarios libres según la disponibilidad del doctor',
      'Resumen claro antes de confirmar',
    ],
    shot: {
      src: SHOTS + '14-assistant-booking-ready.webp',
      alt: 'Flujo para agendar una cita con doctor, fecha y horario seleccionados',
    },
  },
];

export const LANDING_ROLES: LandingRole[] = [
  {
    id: 'assistant',
    label: 'Asistente',
    icon: 'list',
    title: 'Controla todas las citas de la clínica',
    description:
      'Filtra por estado, confirma o cancela con un clic y revisa el historial completo con paginación.',
    shot: {
      src: SHOTS + '13-assistant-appointments.webp',
      alt: 'Listado de citas con estados y acciones de confirmar o cancelar',
    },
  },
  {
    id: 'doctor',
    label: 'Doctor',
    icon: 'stethoscope',
    title: 'Tu horario, tus reglas',
    description:
      'Define tus bloques de atención por día, bloquea vacaciones o congresos y previsualiza los horarios que verán tus pacientes.',
    shot: {
      src: SHOTS + '21-doctor-availability.webp',
      alt: 'Horario semanal de un doctor con bloques de mañana y tarde',
    },
  },
  {
    id: 'admin',
    label: 'Administrador',
    icon: 'staff',
    title: 'Gestiona a tu equipo',
    description:
      'Da de alta doctores y asistentes, asigna especialidades y activa o desactiva accesos cuando lo necesites.',
    shot: {
      src: SHOTS + '31-admin-staff.webp',
      alt: 'Listado del personal de la clínica con roles y especialidades',
    },
  },
];

export const LANDING_STEPS: LandingStep[] = [
  {
    title: 'Registra tu clínica',
    description: 'Crea la cuenta de tu clínica y activa el acceso desde tu correo.',
  },
  {
    title: 'Configura tu equipo',
    description: 'Agrega doctores, asistentes, especialidades y horarios de atención.',
  },
  {
    title: 'Empieza a agendar',
    description: 'Registra pacientes y gestiona todas las citas desde un solo lugar.',
  },
];
