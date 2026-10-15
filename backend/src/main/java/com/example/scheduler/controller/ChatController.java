package com.example.scheduler.controller;

import com.example.scheduler.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat Controller")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/patient")
    @PreAuthorize("hasAnyRole('PATIENT')")
    @Operation(summary = "POST /api/chat/patient — chat to communicate with the AI agent")
    public String patientChat(@RequestBody String message, Authentication auth) {
        return chatService.schedule(message, Long.parseLong(auth.getName()));
    }
}
