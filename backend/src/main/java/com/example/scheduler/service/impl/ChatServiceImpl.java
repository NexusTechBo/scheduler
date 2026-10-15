package com.example.scheduler.service.impl;

import com.example.scheduler.tool.FlowScheduleTool;
import com.example.scheduler.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final HttpSession httpSession;
    private final FlowScheduleTool flowScheduleTool;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, HttpSession httpSession, FlowScheduleTool flowScheduleTool) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a virtual assistant that help to the users schedules their appointments")
                .build();
        this.httpSession = httpSession;
        this.flowScheduleTool = flowScheduleTool;
    }

    @Override
    public String schedule(String message, Long userId) {
        var id = userId + ":" + httpSession.getId();
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, id))
                .tools(flowScheduleTool)
                .call()
                .content();
    }
}
