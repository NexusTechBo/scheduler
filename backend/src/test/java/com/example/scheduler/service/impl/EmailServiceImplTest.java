package com.example.scheduler.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.exception.MailDeliveryException;
import com.example.scheduler.notification.OutgoingEmail;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.AppointmentNotificationType;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.notification.event.StaffCreatedEvent;
import com.example.scheduler.notification.transport.MailTransport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    private static final String FRONTEND_URL = "https://app.example.com";
    private static final String PATIENT_EMAIL = "patient@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String STAFF_EMAIL = "staff@example.com";
    private static final String CLINIC = "Clinica Norte";
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 10, 9, 0);

    @Mock
    private MailTransport mailTransport;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailTransport, "  " + FRONTEND_URL + "//  ");
    }

    private OutgoingEmail captureSent() {
        var captor = ArgumentCaptor.forClass(OutgoingEmail.class);
        verify(mailTransport).send(captor.capture());
        return captor.getValue();
    }

    @Nested
    class SendStaffCredentials {
        @Test
        void shouldSendCredentialsToStaffEmailWhenRecipientPresent() {
            emailService.sendStaffCredentials(staffEvent(STAFF_EMAIL));

            var sent = captureSent();
            assertThat(sent.to()).isEqualTo(STAFF_EMAIL);
            assertThat(sent.subject()).isEqualTo("Tu cuenta en " + CLINIC);
            assertThat(sent.html())
                    .contains("Hola Ana Perez")
                    .contains("ana.perez")
                    .contains("S3cret!pw")
                    .contains("Doctor");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void shouldNotCallTransportWhenRecipientIsBlank(String email) {
            emailService.sendStaffCredentials(staffEvent(email));

            verifyNoInteractions(mailTransport);
        }
    }

    @Nested
    class SendAppointmentConfirmed {
        @Test
        void shouldSendConfirmationToPatientWhenRecipientPresent() {
            emailService.sendAppointmentConfirmed(
                    appointmentEvent(AppointmentNotificationType.CONFIRMED, PATIENT_EMAIL));

            var sent = captureSent();
            assertThat(sent.to()).isEqualTo(PATIENT_EMAIL);
            assertThat(sent.subject()).isEqualTo("Cita confirmada - " + CLINIC);
            assertThat(sent.html())
                    .contains("Hola Juan Lopez")
                    .contains("confirmada")
                    .contains("Dr. House");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        void shouldNotCallTransportWhenPatientEmailIsBlank(String email) {
            emailService.sendAppointmentConfirmed(appointmentEvent(AppointmentNotificationType.CONFIRMED, email));

            verifyNoInteractions(mailTransport);
        }
    }

    @Nested
    class SendAppointmentCancelled {
        @Test
        void shouldSendCancellationWithReasonToPatientWhenRecipientPresent() {
            emailService.sendAppointmentCancelled(
                    appointmentEvent(AppointmentNotificationType.CANCELLED, PATIENT_EMAIL));

            var sent = captureSent();
            assertThat(sent.to()).isEqualTo(PATIENT_EMAIL);
            assertThat(sent.subject()).isEqualTo("Cita cancelada - " + CLINIC);
            assertThat(sent.html()).contains("cancelada").contains("Doctor no disponible");
        }

        @Test
        void shouldNotCallTransportWhenPatientEmailIsNull() {
            emailService.sendAppointmentCancelled(appointmentEvent(AppointmentNotificationType.CANCELLED, null));

            verifyNoInteractions(mailTransport);
        }
    }

    @Nested
    class SendAppointmentRescheduled {
        @Test
        void shouldSendRescheduleToPatientWhenRecipientPresent() {
            emailService.sendAppointmentRescheduled(
                    appointmentEvent(AppointmentNotificationType.RESCHEDULED, PATIENT_EMAIL));

            var sent = captureSent();
            assertThat(sent.to()).isEqualTo(PATIENT_EMAIL);
            assertThat(sent.subject()).isEqualTo("Cita reprogramada - " + CLINIC);
            assertThat(sent.html()).contains("reprogramada").contains("Hola Juan Lopez");
        }

        @Test
        void shouldNotCallTransportWhenPatientEmailIsBlank() {
            emailService.sendAppointmentRescheduled(appointmentEvent(AppointmentNotificationType.RESCHEDULED, " "));

            verifyNoInteractions(mailTransport);
        }
    }

    @Nested
    class SendClinicActivation {
        @Test
        void shouldSendActivationLinkWithNormalizedFrontendUrlWhenRecipientPresent() {
            emailService.sendClinicActivation(activationEvent(ADMIN_EMAIL));

            var sent = captureSent();
            assertThat(sent.to()).isEqualTo(ADMIN_EMAIL);
            assertThat(sent.subject()).isEqualTo("Activa tu clínica " + CLINIC);
            assertThat(sent.html())
                    .contains(FRONTEND_URL + "/activar?token=tok%2Babc")
                    .doesNotContain(FRONTEND_URL + "//");
        }

        @Test
        void shouldNotCallTransportWhenAdminEmailIsNull() {
            emailService.sendClinicActivation(activationEvent(null));

            verifyNoInteractions(mailTransport);
        }
    }

    @Nested
    class SendClinicWelcome {
        @Test
        void shouldSendWelcomeWithLoginLinkWhenRecipientPresent() {
            emailService.sendClinicWelcome(welcomeEvent(ADMIN_EMAIL));

            var sent = captureSent();
            assertThat(sent.to()).isEqualTo(ADMIN_EMAIL);
            assertThat(sent.subject()).isEqualTo("Tu clínica " + CLINIC + " está activa");
            assertThat(sent.html()).contains(FRONTEND_URL + "/login").contains("norte.admin");
        }

        @Test
        void shouldNotCallTransportWhenAdminEmailIsEmpty() {
            emailService.sendClinicWelcome(welcomeEvent(""));

            verifyNoInteractions(mailTransport);
        }
    }

    @Nested
    class FailureHandling {
        @Test
        @DisplayName("Delivery failures from the transport are logged and swallowed")
        void shouldNotPropagateWhenTransportThrowsMailDeliveryException() {
            doThrow(new MailDeliveryException("Brevo responded 401: Key not found", null))
                    .when(mailTransport)
                    .send(any());

            assertThatCode(() -> emailService.sendClinicActivation(activationEvent(ADMIN_EMAIL)))
                    .doesNotThrowAnyException();
            verify(mailTransport).send(any());
        }

        @Test
        void shouldNotPropagateWhenTransportThrowsUnexpectedRuntimeException() {
            doThrow(new IllegalStateException("boom")).when(mailTransport).send(any());

            assertThatCode(() -> emailService.sendAppointmentConfirmed(
                            appointmentEvent(AppointmentNotificationType.CONFIRMED, PATIENT_EMAIL)))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldNotCallTransportNorPropagateWhenTemplateRenderingFails() {
            // A null start time makes the appointment template fail while rendering.
            var event = new AppointmentNotificationEvent(
                    AppointmentNotificationType.CONFIRMED,
                    AppointmentStatus.CONFIRMED,
                    1L,
                    CLINIC,
                    "norte",
                    "Dr. House",
                    null,
                    "Juan Lopez",
                    PATIENT_EMAIL,
                    null,
                    null,
                    null,
                    null,
                    AppointmentNotificationEvent.ALL_CHANNELS);

            assertThatCode(() -> emailService.sendAppointmentConfirmed(event)).doesNotThrowAnyException();
            verifyNoInteractions(mailTransport);
        }
    }

    private static StaffCreatedEvent staffEvent(String email) {
        return new StaffCreatedEvent(email, "Ana Perez", "ana.perez", "S3cret!pw", ERole.DOCTOR, CLINIC);
    }

    private static AppointmentNotificationEvent appointmentEvent(AppointmentNotificationType type, String email) {
        return new AppointmentNotificationEvent(
                type,
                AppointmentStatus.CONFIRMED,
                42L,
                CLINIC,
                "norte",
                "Dr. House",
                "Cardiologia",
                "Juan Lopez",
                email,
                "+59170000000",
                START,
                START.plusMinutes(30),
                "Doctor no disponible",
                AppointmentNotificationEvent.ALL_CHANNELS);
    }

    private static ClinicActivationEvent activationEvent(String email) {
        return new ClinicActivationEvent(7L, CLINIC, "Admin Norte", email, "tok+abc", START.plusDays(2));
    }

    private static ClinicActivatedEvent welcomeEvent(String email) {
        return new ClinicActivatedEvent(7L, CLINIC, "Admin Norte", email, "norte.admin");
    }
}
