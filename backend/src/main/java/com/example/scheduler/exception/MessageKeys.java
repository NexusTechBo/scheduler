package com.example.scheduler.exception;

public final class MessageKeys {
    public static final String AUTH_INVALID_CREDENTIALS = "auth.invalidCredentials";
    public static final String AUTH_USER_NOT_ACTIVE = "auth.userNotActive";
    public static final String AUTH_CLINIC_NOT_ACTIVATED = "auth.clinicNotActivated";

    public static final String SECURITY_AUTHENTICATION_REQUIRED = "security.authenticationRequired";
    public static final String SECURITY_ACCESS_DENIED = "security.accessDenied";
    public static final String COMMON_FORBIDDEN = "common.forbidden";

    public static final String RATE_LIMIT_EXCEEDED = "rateLimit.exceeded";
    public static final String RATE_LIMIT_EXCEEDED_MINUTES = "rateLimit.exceededMinutes";

    public static final String CLINIC_NOT_FOUND = "clinic.notFound";
    public static final String CLINIC_ALREADY_EXISTS = "clinic.alreadyExists";
    public static final String CLINIC_SLUG_TAKEN = "clinic.slugTaken";
    public static final String CLINIC_ACTIVATION_LINK_INVALID = "clinic.activationLinkInvalid";

    public static final String PERSONAL_NOT_FOUND = "personal.notFound";
    public static final String PERSONAL_DOCTOR_NOT_FOUND = "personal.doctorNotFound";
    public static final String PERSONAL_DOCTOR_ONLY_ACTIVE = "personal.doctorOnlyActive";
    public static final String PERSONAL_ALREADY_EXISTS = "personal.alreadyExists";
    public static final String PERSONAL_USERNAME_TAKEN = "personal.usernameTaken";
    public static final String PERSONAL_EMAIL_ALREADY_EXISTS = "personal.emailAlreadyExists";
    public static final String PERSONAL_PHONE_ALREADY_EXISTS = "personal.phoneAlreadyExists";
    public static final String PERSONAL_CURRENT_PASSWORD_INCORRECT = "personal.currentPasswordIncorrect";
    public static final String PERSONAL_CANNOT_DEACTIVATE_SELF = "personal.cannotDeactivateSelf";
    public static final String PERSONAL_DOCTOR_HAS_UPCOMING_APPOINTMENTS = "personal.doctorHasUpcomingAppointments";
    public static final String PERSONAL_DOCTOR_HAS_ACTIVE_AVAILABILITIES = "personal.doctorHasActiveAvailabilities";

    public static final String PATIENT_NOT_FOUND = "patient.notFound";
    public static final String PATIENT_NOT_FOUND_PHONE = "patient.notFoundPhone";
    public static final String PATIENT_ALREADY_EXISTS = "patient.alreadyExists";
    public static final String PATIENT_EMAIL_ALREADY_EXISTS = "patient.emailAlreadyExists";
    public static final String PATIENT_PHONE_ALREADY_EXISTS = "patient.phoneAlreadyExists";

    public static final String SPECIALTY_NOT_FOUND = "specialty.notFound";
    public static final String SPECIALTY_ALREADY_EXISTS = "specialty.alreadyExists";

    public static final String AVAILABILITY_NOT_FOUND = "availability.notFound";
    public static final String AVAILABILITY_OVERLAPS = "availability.overlaps";
    public static final String AVAILABILITY_UNAUTHORIZED = "availability.unauthorized";

    public static final String SCHEDULE_EXCEPTION_NOT_FOUND = "scheduleException.notFound";
    public static final String SCHEDULE_EXCEPTION_DATE_PAST = "scheduleException.date.past";
    public static final String SCHEDULE_EXCEPTION_ACTIVE_APPOINTMENTS_CONFLICT =
            "scheduleException.activeAppointmentsConflict";
    public static final String SCHEDULE_EXCEPTION_DEFAULT_CANCELLATION_REASON =
            "scheduleException.defaultCancellationReason";

    public static final String APPOINTMENT_NOT_FOUND = "appointment.notFound";
    public static final String APPOINTMENT_OUT_OF_JOURNEY = "appointment.outOfJourney";
    public static final String APPOINTMENT_PAST_DATE = "appointment.pastDate";
    public static final String APPOINTMENT_SCHEDULE_BLOCKED = "appointment.scheduleBlocked";
    public static final String APPOINTMENT_SLOT_TAKEN = "appointment.slotTaken";
    public static final String APPOINTMENT_CONFIRM_ONLY_PENDING = "appointment.confirmOnlyPending";
    public static final String APPOINTMENT_ALREADY_CANCELLED = "appointment.alreadyCancelled";
    public static final String APPOINTMENT_UNAUTHORIZED = "appointment.unauthorized";

    public static final String GUEST_CLINIC_UNAVAILABLE = "guest.clinicUnavailable";
    public static final String GUEST_CANNOT_BOOK = "guest.cannotBook";

    public static final String VALIDATION_PHONE_INVALID = "validation.phone.invalid";

    public static final String ERROR_VALIDATION_FAILED = "error.validation.failed";
    public static final String ERROR_HTTP_MALFORMED_BODY = "error.http.malformedBody";
    public static final String ERROR_HTTP_RESOURCE_NOT_FOUND = "error.http.resourceNotFound";
    public static final String ERROR_HTTP_METHOD_NOT_SUPPORTED = "error.http.methodNotSupported";
    public static final String ERROR_HTTP_MEDIA_TYPE_NOT_SUPPORTED = "error.http.mediaTypeNotSupported";
    public static final String ERROR_PARAM_INVALID_TYPE = "error.param.invalidType";
    public static final String ERROR_PARAM_MISSING = "error.param.missing";
    public static final String ERROR_PARAM_INVALID_SORT_PROPERTY = "error.param.invalidSortProperty";
    public static final String ERROR_PARAM_INVALID_SORT = "error.param.invalidSort";
    public static final String ERROR_DATA_CONFLICT = "error.data.conflict";
    public static final String ERROR_DATA_OPTIMISTIC_LOCK = "error.data.optimisticLock";
    public static final String ERROR_SERVER_INTERNAL = "error.server.internal";

    private MessageKeys() {}
}
