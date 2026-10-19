package com.example.scheduler.websocket;

public record AppointmentChangedEvent(
        Long clinicId,
        BoardEvent payload
) {

    public static String boardTopic(Long clinicId) {
        return "/topic/clinic/" + clinicId + "/board";
    }
}
