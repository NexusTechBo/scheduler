package com.example.scheduler.service.impl;

import com.example.scheduler.notification.AppointmentNotificationEvent;
import com.example.scheduler.notification.EmailTemplates;
import com.example.scheduler.notification.EmailTemplates.EmailContent;
import com.example.scheduler.notification.StaffCreatedEvent;
import com.example.scheduler.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {
    static final String STAFF_CREDENTIALS = "STAFF_CREDENTIALS";
    static final String APPOINTMENT_CONFIRMED = "APPOINTMENT_CONFIRMED";
    static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
    private static final Pattern EMAIL_IN_TEXT = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+)");
    private static final Pattern LINE_BREAKS = Pattern.compile("[\\r\\n]+\\s*");

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${app.mail.from}") String from,
                            @Value("${app.mail.from-name:Scheduler}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
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

    private void sendToPatient(String type, AppointmentNotificationEvent event,
                               BiFunction<AppointmentNotificationEvent, String, EmailContent> template) {
        var ref = "appointment=" + event.appointmentId() + " recipient=patient";
        send(type, ref, event.patientEmail(), () -> template.apply(event, event.patientName()));
    }

    private void send(String type, String ref, String to, Supplier<EmailContent> contentSupplier) {
        if (!StringUtils.hasText(to)) {
            log.warn("Email {} skipped: recipient has no email address [{}]", type, ref);
            return;
        }
        var maskedTo = maskEmail(to);
        try {
            var content = contentSupplier.get();
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(content.subject());
            helper.setText(content.html(), true);
            mailSender.send(message);
            log.info("Email {} sent to {} [{}]", type, maskedTo, ref);
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException e) {
            logFailure(type, maskedTo, ref, e);
        }
    }

    private void logFailure(String type, String maskedTo, String ref, Exception e) {
        log.error("Email {} to {} failed [{}]: {}: {}", type, maskedTo, ref, e.getClass().getSimpleName(), sanitizeForLog(e.getMessage()));
        log.debug("Email {} to {} failure stack trace [{}]", type, maskedTo, ref, e);
    }

    static String maskEmail(String email) {
        var at = email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }

    static String sanitizeForLog(String text) {
        if (text == null) return "";
        var masked = EMAIL_IN_TEXT.matcher(text).replaceAll("$1***@$2");
        return LINE_BREAKS.matcher(masked).replaceAll(" ").strip();
    }
}
