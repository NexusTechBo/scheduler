package com.example.scheduler.notification.template;

import com.example.scheduler.config.LocaleConfig;
import com.example.scheduler.enums.ERole;
import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import com.example.scheduler.notification.event.ClinicActivatedEvent;
import com.example.scheduler.notification.event.ClinicActivationEvent;
import com.example.scheduler.notification.event.StaffCreatedEvent;
import com.example.scheduler.util.Utils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

public final class EmailTemplates {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LocaleConfig.SPANISH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", LocaleConfig.SPANISH);

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
                        Utils.escapeHtml(event.name()),
                        Utils.escapeHtml(clinicLabel(event.clinicName())),
                        Utils.escapeHtml(event.username()),
                        Utils.escapeHtml(event.rawPassword()),
                        Utils.escapeHtml(roleLabel(event.role())));
        return new EmailContent(subject, layout(body));
    }

    public static EmailContent clinicActivation(ClinicActivationEvent event, String frontendUrl) {
        var clinic = clinicLabel(event.clinicName());
        var subject = "Activa tu clínica " + clinic;
        var link = frontendUrl + "/activar?token=" + URLEncoder.encode(event.token(), StandardCharsets.UTF_8);
        var body = """
                <p>Hola %s,</p>
                <p>Gracias por registrar <strong>%s</strong>. Para activar tu clínica y poder iniciar sesión, confirma tu correo con el siguiente botón:</p>
                %s
                <p>Si el botón no funciona, copia y pega este enlace en tu navegador:<br>%s</p>
                <p>Este enlace vence en 48 horas. Si no solicitaste este registro, puedes ignorar este correo.</p>
                """.formatted(
                        Utils.escapeHtml(event.adminName()),
                        Utils.escapeHtml(clinic),
                        button(link, "Activar clínica"),
                        anchor(link));
        return new EmailContent(subject, layout(body));
    }

    public static EmailContent clinicWelcome(ClinicActivatedEvent event, String frontendUrl) {
        var clinic = clinicLabel(event.clinicName());
        var subject = "Tu clínica " + clinic + " está activa";
        var link = frontendUrl + "/login";
        var body = """
                <p>Hola %s,</p>
                <p>Tu clínica <strong>%s</strong> ya está activa. Ya puedes iniciar sesión con tu usuario:</p>
                <table cellpadding="4">%s</table>
                %s
                <p>Si el botón no funciona, copia y pega este enlace en tu navegador:<br>%s</p>
                """.formatted(
                        Utils.escapeHtml(event.adminName()),
                        Utils.escapeHtml(clinic),
                        row("Usuario", event.adminUsername()),
                        button(link, "Iniciar sesión"),
                        anchor(link));
        return new EmailContent(subject, layout(body));
    }

    public static EmailContent appointmentConfirmed(AppointmentNotificationEvent event, String recipientName) {
        var subject = "Cita confirmada - " + clinicLabel(event.clinicName());
        var body = """
                <p>Hola %s,</p>
                <p>La siguiente cita fue <strong>confirmada</strong>:</p>
                %s
                """.formatted(Utils.escapeHtml(recipientName), appointmentDetails(event));
        return new EmailContent(subject, layout(body));
    }

    public static EmailContent appointmentCancelled(AppointmentNotificationEvent event, String recipientName) {
        var subject = "Cita cancelada - " + clinicLabel(event.clinicName());
        var reason = StringUtils.hasText(event.cancellationReason())
                ? "<p><strong>Motivo:</strong> " + Utils.escapeHtml(event.cancellationReason()) + "</p>"
                : "";
        var body = """
                <p>Hola %s,</p>
                <p>La siguiente cita fue <strong>cancelada</strong>:</p>
                %s
                %s
                """.formatted(Utils.escapeHtml(recipientName), appointmentDetails(event), reason);
        return new EmailContent(subject, layout(body));
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DATE_FORMATTER);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime.format(TIME_FORMATTER);
    }

    private static String appointmentDetails(AppointmentNotificationEvent event) {
        var specialtyRow = StringUtils.hasText(event.specialtyName()) ? row("Especialidad", event.specialtyName()) : "";
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
        return "<tr><td><strong>" + label + ":</strong></td><td>" + Utils.escapeHtml(value) + "</td></tr>";
    }

    private static String button(String href, String label) {
        return "<p><a href=\"" + Utils.escapeHtml(href)
                + "\" style=\"display: inline-block; padding: 10px 20px; background-color: #2563eb; "
                + "color: #ffffff; text-decoration: none; border-radius: 6px;\">" + Utils.escapeHtml(label)
                + "</a></p>";
    }

    private static String anchor(String href) {
        var url = Utils.escapeHtml(href);
        return "<a href=\"" + url + "\">" + url + "</a>";
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

    private static String clinicLabel(String clinicName) {
        var name =
                clinicName == null ? null : Utils.stripControlChars(clinicName).strip();
        return StringUtils.hasText(name) ? name : "la clínica";
    }

    private static String roleLabel(ERole role) {
        if (role == null) return "";
        return switch (role) {
            case ADMINISTRATOR -> "Administrador";
            case DOCTOR -> "Doctor";
            case ASSISTANT -> "Asistente";
            case GUEST -> "Invitado";
        };
    }
}
