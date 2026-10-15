package com.example.scheduler.websocket;

import com.example.scheduler.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP CONNECT frame with the REST JWT and only allows subscribing to the caller's own clinic board.
 */
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    static final String CLINIC_ID = "clinicId";

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> throw new MessageDeliveryException("Sending is not supported"); // push-only
            default -> { }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        var header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new MessageDeliveryException("Missing bearer token");
        var claims = jwtUtil.parse(header.substring(7))
                .orElseThrow(() -> new MessageDeliveryException("Invalid token"));
        var clinicId = claims.get(CLINIC_ID, Long.class);
        if (clinicId == null || accessor.getSessionAttributes() == null)
            throw new MessageDeliveryException("Invalid token");
        accessor.getSessionAttributes().put(CLINIC_ID, clinicId);
        var role = claims.get("role", String.class);
        var authorities = role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role)) : List.<SimpleGrantedAuthority>of();
        accessor.setUser(new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        var attrs = accessor.getSessionAttributes();
        if (accessor.getUser() == null || attrs == null || !(attrs.get(CLINIC_ID) instanceof Long clinicId))
            throw new MessageDeliveryException("Not authenticated");
        if (!AppointmentChangedEvent.boardTopic(clinicId).equals(accessor.getDestination()))
            throw new MessageDeliveryException("Forbidden destination");
    }
}
