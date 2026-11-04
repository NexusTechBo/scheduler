package com.example.scheduler.notification.template;

import com.example.scheduler.notification.event.AppointmentNotificationEvent;
import org.springframework.util.StringUtils;

public final class WhatsappTemplates {
    private WhatsappTemplates() {}

    public static String appointmentConfirmed(AppointmentNotificationEvent event) {
        return """
                Hola %s, tu cita en *%s* fue *confirmada*.

                %s""".formatted(event.patientName(), clinicLabel(event.clinicName()), appointmentDetails(event));
    }

    public static String appointmentCancelled(AppointmentNotificationEvent event) {
        var reason = StringUtils.hasText(event.cancellationReason())
                ? "\n\nMotivo: " + event.cancellationReason().strip()
                : "";
        return """
                Hola %s, tu cita en *%s* fue *cancelada*.

                %s%s""".formatted(event.patientName(), clinicLabel(event.clinicName()), appointmentDetails(event), reason);
    }

    private static String appointmentDetails(AppointmentNotificationEvent event) {
        var time = EmailTemplates.formatTime(event.startTime())
                + (event.endTime() != null ? " - " + EmailTemplates.formatTime(event.endTime()) : "");
        var details = new StringBuilder()
                .append("Doctor: ")
                .append(event.doctorName())
                .append('\n');
        if (StringUtils.hasText(event.specialtyName()))
            details.append("Especialidad: ").append(event.specialtyName()).append('\n');
        return details.append("Fecha: ")
                .append(EmailTemplates.formatDate(event.startTime()))
                .append('\n')
                .append("Hora: ")
                .append(time)
                .toString();
    }

    private static String clinicLabel(String clinicName) {
        return StringUtils.hasText(clinicName) ? clinicName.strip() : "la clínica";
    }
}
