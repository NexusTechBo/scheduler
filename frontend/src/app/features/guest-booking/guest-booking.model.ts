export const GUEST_BOOKING_STEPS = ['doctor', 'slot', 'patient', 'summary', 'done'] as const;

export type GuestBookingStep = (typeof GUEST_BOOKING_STEPS)[number];
