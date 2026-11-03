export const GUEST_BOOKING_STEPS = ['doctor', 'slot', 'patient', 'summary', 'done'] as const;

export const STEP_TITLES: Record<GuestBookingStep, string> = {
  doctor: 'Elige tu doctor',
  slot: 'Elige fecha y horario',
  patient: 'Tus datos',
  summary: 'Revisa tu reserva',
  done: '¡Reserva registrada!',
};

export type GuestBookingStep = (typeof GUEST_BOOKING_STEPS)[number];
