package com.example.scheduler.notification;

import com.example.scheduler.enums.ERole;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailTemplates {
    private static final Locale SPANISH = Locale.forLanguageTag("es");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", SPANISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", SPANISH);
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");

    public record EmailContent(String subject, String html) {}

    private EmailTemplates() {}

    public static EmailContent staffCredentials(StaffCreatedEvent event) {
        var subject = "Tu cuenta en " + clinicLabel(event.clinicName());
        var body = """
                <p>Hola %s,</p>
                <p>Se creó tu cuenta en <strong>%s</strong>. Estos son tus datos de acceso:</p>
                <table cellpadding="4">
                  <tr><td><strong>Usuario:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Contraseña:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Rol:</strong></td><td>%s</td></tr>
                </table>
                <p>Te recomendamos cambiar tu contraseña después de iniciar sesión por primera vez.</p>
                """.formatted(
                escape(event.name()),
                escape(clinicLabel(event.clinicName())),
                escape(event.username()),
                escape(event.rawPassword()),
                escape(roleLabel(event.role())));
        return new EmailContent(subject, layout(body));
    }

    public static EmailContent appointmentConfirmed(AppointmentNotificationEvent event, String recipientName) {
        var subject = "Cita confirmada - " + clinicLabel(event.clinicName());
        var body = """
                <p>Hola %s,</p>
                <p>La siguiente cita fue <strong>confirmada</strong>:</p>
                %s
                """.formatted(escape(recipientName), appointmentDetails(event));
        return new EmailContent(subject, layout(body));
    }

    public static EmailContent appointmentCancelled(AppointmentNotificationEvent event, String recipientName) {
        var subject = "Cita cancelada - " + clinicLabel(event.clinicName());
        var reason = StringUtils.hasText(event.cancellationReason())
                ? "<p><strong>Motivo:</strong> " + escape(event.cancellationReason()) + "</p>"
                : "";
        var body = """
                <p>Hola %s,</p>
                <p>La siguiente cita fue <strong>cancelada</strong>:</p>
                %s
                %s
                """.formatted(escape(recipientName), appointmentDetails(event), reason);
        return new EmailContent(subject, layout(body));
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime.format(TIME_FORMATTER);
    }

    private static String appointmentDetails(AppointmentNotificationEvent event) {
        var specialtyRow = StringUtils.hasText(event.specialtyName())
                ? row("Especialidad", event.specialtyName())
                : "";
        var time = formatTime(event.startTime()) + (event.endTime() != null ? " - " + formatTime(event.endTime()) : "");
        return "<table cellpadding=\"4\">"
                + row("Clínica", clinicLabel(event.clinicName()))
                + row("Doctor", event.doctorName())
                + specialtyRow
                + row("Paciente", event.patientName())
                + row("Fecha", formatDate(event.startTime()))
                + row("Hora", time)
                + "</table>";
    }

    private static String row(String label, String value) {
        return "<tr><td><strong>" + label + ":</strong></td><td>" + escape(value) + "</td></tr>";
    }

    private static String layout(String body) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; color: #1f2937;">
                %s
                <p style="color: #6b7280; font-size: 12px;">Este es un mensaje automático, por favor no respondas a este correo.</p>
                </body>
                </html>
                """.formatted(body);
    }

    /** Control characters are stripped so user-provided names can't inject line breaks into headers (subject). */
    private static String clinicLabel(String clinicName) {
        var name = clinicName == null ? null : CONTROL_CHARS.matcher(clinicName).replaceAll("").strip();
        return StringUtils.hasText(name) ? name : "la clínica";
    }

    private static String roleLabel(ERole role) {
        if (role == null) return "";
        return switch (role) {
            case ADMINISTRATOR -> "Administrador";
            case DOCTOR -> "Doctor";
            case ASSISTANT -> "Asistente";
        };
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value, "UTF-8");
    }
}
