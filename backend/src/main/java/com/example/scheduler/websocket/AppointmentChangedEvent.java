package com.example.scheduler.websocket;

/** Domain event published inside the write transaction; pushed to the clinic board after commit. */
public record AppointmentChangedEvent(Long clinicId, BoardEvent payload) {

    public static String boardTopic(Long clinicId) {
        return "/topic/clinic/" + clinicId + "/board";
    }
}
