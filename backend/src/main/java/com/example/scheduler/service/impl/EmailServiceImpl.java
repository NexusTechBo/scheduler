package com.example.scheduler.service.impl;

import com.example.scheduler.notification.EmailContent;
import com.example.scheduler.notification.OutgoingEmail;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.notification.event.StaffCreatedEvent;
import com.example.scheduler.notification.template.EmailTemplates;
import com.example.scheduler.notification.transport.MailTransport;
import com.example.scheduler.service.EmailService;
import com.example.scheduler.util.LogUtils;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    static final String STAFF_CREDENTIALS = "STAFF_CREDENTIALS";
    static final String APPOINTMENT_CONFIRMED = "APPOINTMENT_CONFIRMED";
    static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
    static final String APPOINTMENT_RESCHEDULED = "APPOINTMENT_RESCHEDULED";
    static final String CLINIC_ACTIVATION = "CLINIC_ACTIVATION";
    static final String CLINIC_WELCOME = "CLINIC_WELCOME";
    private static final Pattern TRAILING_SLASHES = Pattern.compile("/+$");

    private final MailTransport mailTransport;
    private final String frontendUrl;

    public EmailServiceImpl(MailTransport mailTransport, @Value("${app.frontend-url}") String frontendUrl) {
        this.mailTransport = mailTransport;
        this.frontendUrl = TRAILING_SLASHES.matcher(frontendUrl.strip()).replaceAll("");
    }

    @Override
    public void sendStaffCredentials(StaffCreatedEvent event) {
        send(STAFF_CREDENTIALS, "staff", event.email(), () -> EmailTemplates.staffCredentials(event));
    }

    @Override
    public void sendAppointmentConfirmed(AppointmentNotificationEvent event) {
        sendToPatient(APPOINTMENT_CONFIRMED, event, EmailTemplates::appointmentConfirmed);
    }

    @Override
    public void sendAppointmentCancelled(AppointmentNotificationEvent event) {
        sendToPatient(APPOINTMENT_CANCELLED, event, EmailTemplates::appointmentCancelled);
    }

    @Override
    public void sendAppointmentRescheduled(AppointmentNotificationEvent event) {
        sendToPatient(APPOINTMENT_RESCHEDULED, event, EmailTemplates::appointmentRescheduled);
    }

    @Override
    public void sendClinicActivation(ClinicActivationEvent event) {
        send(
                CLINIC_ACTIVATION,
                clinicAdminRef(event.clinicId()),
                event.adminEmail(),
                () -> EmailTemplates.clinicActivation(event, frontendUrl));
    }

    @Override
    public void sendClinicWelcome(ClinicActivatedEvent event) {
        send(
                CLINIC_WELCOME,
                clinicAdminRef(event.clinicId()),
                event.adminEmail(),
                () -> EmailTemplates.clinicWelcome(event, frontendUrl));
    }

    private void sendToPatient(
            String type,
            AppointmentNotificationEvent event,
            BiFunction<AppointmentNotificationEvent, String, EmailContent> template) {
        var ref = "appointment=" + event.appointmentId() + " recipient=patient";
        send(type, ref, event.patientEmail(), () -> template.apply(event, event.patientName()));
    }

    private void send(String type, String ref, String to, Supplier<EmailContent> contentSupplier) {
        if (!StringUtils.hasText(to)) {
            log.warn("Email {} skipped: recipient has no email address [{}]", type, ref);
            return;
        }
        var maskedTo = LogUtils.maskEmail(to);
        try {
            var content = contentSupplier.get();
            mailTransport.send(new OutgoingEmail(to, content.subject(), content.html()));
            log.info("Email {} sent to {} [{}]", type, maskedTo, ref);
        } catch (RuntimeException e) {
            logFailure(type, maskedTo, ref, e);
        }
    }

    private void logFailure(String type, String maskedTo, String ref, Exception e) {
        log.error(
                "Email {} to {} failed [{}]: {}: {}",
                type,
                maskedTo,
                ref,
                e.getClass().getSimpleName(),
                LogUtils.sanitizeForLog(e.getMessage()));
    }

    private static String clinicAdminRef(Long clinicId) {
        return "clinic=" + clinicId + " recipient=admin";
    }
}
