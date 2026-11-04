package com.example.scheduler.websocket.event;

import java.util.Objects;
import java.util.regex.Pattern;

public record AppointmentChangedEvent(Long clinicId, Long doctorId, BoardEvent payload) {
    private static final String CLINIC_PREFIX = "/topic/clinic/";
    private static final String BOARD_SUFFIX = "/board";
    private static final Pattern DOCTOR_BOARD_TOPIC = Pattern.compile("^/topic/clinic/(\\d+)/doctor/(\\d+)/board$");

    public AppointmentChangedEvent {
        Objects.requireNonNull(clinicId, "clinicId");
        Objects.requireNonNull(doctorId, "doctorId");
        Objects.requireNonNull(payload, "payload");
    }

    public static String boardTopic(Long clinicId) {
        return CLINIC_PREFIX + clinicId + BOARD_SUFFIX;
    }

    public static String doctorBoardTopic(Long clinicId, Long doctorId) {
        return CLINIC_PREFIX + clinicId + "/doctor/" + doctorId + BOARD_SUFFIX;
    }

    public static boolean isDoctorBoardTopicOfClinic(Long clinicId, String destination) {
        if (destination == null) return false;
        var matcher = DOCTOR_BOARD_TOPIC.matcher(destination);
        return matcher.matches() && matcher.group(1).equals(String.valueOf(clinicId));
    }
}
