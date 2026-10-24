package com.example.scheduler.service.impl;

import com.example.scheduler.enums.AppointmentStatus;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.notification.AppointmentNotificationEvent;
import com.example.scheduler.notification.ClinicActivatedEvent;
import com.example.scheduler.notification.ClinicActivationEvent;
import com.example.scheduler.notification.StaffCreatedEvent;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 21, 9, 30);

    private JavaMailSender mailSender;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(_ -> new MimeMessage(Session.getInstance(new Properties())));
        emailService = new EmailServiceImpl(mailSender, "no-reply@scheduler.local", "Scheduler", "https://app.example.com//");
    }

    @Test
    void clinicActivationContainsEncodedLinkAndEscapesUserValues() throws Exception {
        emailService.sendClinicActivation(new ClinicActivationEvent(
                7L, "Clínica <Sur>", "Ana <b>Pérez</b>", "ana@mail.com", "ab+c/d=e", START));

        var message = captureSent(1).getFirst();
        var html = (String) message.getContent();
        assertThat(recipient(message)).isEqualTo("ana@mail.com");
        assertThat(message.getSubject()).isEqualTo("Activa tu clínica Clínica <Sur>");
        assertThat(html)
                .contains("https://app.example.com/activar?token=ab%2Bc%2Fd%3De", "48 horas", "Ana &lt;b&gt;P", "Clínica &lt;Sur&gt;")
                .doesNotContain("<b>Pérez</b>", "ab+c/d=e", "<Sur>", ".com//activar");
    }

    @Test
    void clinicWelcomeContainsUsernameAndLoginLink() throws Exception {
        emailService.sendClinicWelcome(new ClinicActivatedEvent(
                7L, "Clínica Sur", "Ana", "ana@mail.com", "admin@clinicasur.com"));

        var message = captureSent(1).getFirst();
        assertThat(recipient(message)).isEqualTo("ana@mail.com");
        assertThat(message.getSubject()).isEqualTo("Tu clínica Clínica Sur está activa");
        assertThat((String) message.getContent())
                .contains("Hola Ana", "admin@clinicasur.com", "href=\"https://app.example.com/login\"");
    }

    @Test
    void clinicActivationSubjectStripsControlCharacters() throws Exception {
        emailService.sendClinicActivation(new ClinicActivationEvent(
                7L, "Evil\r\nBcc: x@y.com", "Ana", "ana@mail.com", "token", START));

        assertThat(captureSent(1).getFirst().getSubject()).isEqualTo("Activa tu clínica EvilBcc: x@y.com");
    }

    @Test
    void clinicActivationEventToStringHidesToken() {
        var event = new ClinicActivationEvent(7L, "C", "Ana", "a@mail.com", "secretToken", START);
        assertThat(event.toString()).doesNotContain("secretToken");
    }

    @Test
    void staffCredentialsContainsLoginDataAndEscapesUserValues() throws Exception {
        emailService.sendStaffCredentials(new StaffCreatedEvent(
                "ana@mail.com", "Ana <b>Pérez</b>", "ana@clinica.com", "S3cret&1", ERole.DOCTOR, "Clínica Sur"));

        var message = captureSent(1).getFirst();
        var html = (String) message.getContent();
        assertThat(recipient(message)).isEqualTo("ana@mail.com");
        assertThat(((InternetAddress) message.getFrom()[0]).getPersonal()).isEqualTo("Scheduler");
        assertThat(message.getSubject()).isEqualTo("Tu cuenta en Clínica Sur");
        assertThat(html)
                .contains("ana@clinica.com", "S3cret&amp;1", "Doctor", "Ana &lt;b&gt;P")
                .doesNotContain("<b>Pérez</b>");
    }

    @Test
    void appointmentConfirmedSendsOnlyToPatientInSpanish() throws Exception {
        emailService.sendAppointmentConfirmed(appointment(AppointmentStatus.CONFIRMED, null));

        var message = captureSent(1).getFirst();
        assertThat(recipient(message)).isEqualTo("patient@mail.com");
        assertThat(message.getSubject()).isEqualTo("Cita confirmada - Clínica Sur");
        assertThat((String) message.getContent())
                .contains("Hola Juan", "confirmada", "Dra. Rosa", "Cardiología", "miércoles 21 de octubre de 2026", "09:30 - 10:00");
    }

    @Test
    void appointmentCancelledSendsOnlyToPatientWithEscapedReason() throws Exception {
        emailService.sendAppointmentCancelled(appointment(AppointmentStatus.CANCELLED, "Viaje <urgente>"));

        var message = captureSent(1).getFirst();
        assertThat(recipient(message)).isEqualTo("patient@mail.com");
        assertThat(message.getSubject()).isEqualTo("Cita cancelada - Clínica Sur");
        assertThat((String) message.getContent())
                .contains("Hola Juan", "Dra. Rosa", "Motivo:", "Viaje &lt;urgente&gt;");
    }

    @Test
    void sendFailureIsContainedAndNeverThrows() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        emailService.sendAppointmentCancelled(appointment(AppointmentStatus.CANCELLED, null));

        captureSent(1);
    }

    @Test
    void templateFailureIsContainedAndNeverThrows() {
        var broken = new AppointmentNotificationEvent(
                AppointmentStatus.CONFIRMED,
                1L,
                "Clínica Sur",
                "Dra. Rosa",
                null,
                "Juan",
                "patient@mail.com",
                null,
                null,
                null
        );
        emailService.sendAppointmentConfirmed(broken);
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void controlCharactersAreStrippedFromSubject() throws Exception {
        emailService.sendStaffCredentials(new StaffCreatedEvent(
                "ana@mail.com",
                "Ana",
                "ana@c.com",
                "secret12",
                ERole.ADMINISTRATOR,
                "Evil\r\nBcc: x@y.com"
        ));

        assertThat(captureSent(1).getFirst().getSubject()).isEqualTo("Tu cuenta en EvilBcc: x@y.com");
    }

    @Test
    void maskEmailHidesLocalPart() {
        assertThat(EmailServiceImpl.maskEmail("ana.perez@mail.com")).isEqualTo("a***@mail.com");
        assertThat(EmailServiceImpl.maskEmail("invalid")).isEqualTo("***");
    }

    @Test
    void sanitizeForLogMasksEmailsAndKeepsOneLine() {
        assertThat(EmailServiceImpl.sanitizeForLog("550 <ana.perez@mail.com>: Recipient address rejected"))
                .isEqualTo("550 <a***@mail.com>: Recipient address rejected");
        assertThat(EmailServiceImpl.sanitizeForLog("Mail server connection failed.\r\n  Read timed out"))
                .isEqualTo("Mail server connection failed. Read timed out");
        assertThat(EmailServiceImpl.sanitizeForLog(null)).isEmpty();
    }

    @Test
    void missingRecipientIsSkipped() {
        emailService.sendStaffCredentials(new StaffCreatedEvent(
                null,
                "Ana",
                "ana@clinica.com",
                "secret",
                ERole.ASSISTANT,
                "Clínica Sur"
        ));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void staffEventToStringHidesPassword() {
        var event = new StaffCreatedEvent("a@mail.com", "Ana", "ana@c.com", "topSecret", ERole.ADMINISTRATOR, "C");
        assertThat(event.toString()).doesNotContain("topSecret");
    }

    private List<MimeMessage> captureSent(int expected) {
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(expected)).send(captor.capture());
        return captor.getAllValues();
    }

    private static String recipient(MimeMessage message) {
        try {
            return message.getRecipients(Message.RecipientType.TO)[0].toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static AppointmentNotificationEvent appointment(AppointmentStatus status, String reason) {
        return new AppointmentNotificationEvent(
                status,
                1L,
                "Clínica Sur",
                "Dra. Rosa",
                "Cardiología",
                "Juan",
                "patient@mail.com",
                START,
                START.plusMinutes(30),
                reason
        );
    }
}
