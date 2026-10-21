package com.example.scheduler.websocket;

import com.example.scheduler.config.tenant.TenantContext;
import com.example.scheduler.security.ActiveUserChecker;
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

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final ActiveUserChecker activeUserChecker;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;
        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            case SEND -> throw new MessageDeliveryException("Sending is not supported");
            default -> { }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        var header = accessor.getFirstNativeHeader("Authorization");
        var attrs = accessor.getSessionAttributes();
        if (header == null || !header.startsWith("Bearer "))
            throw new MessageDeliveryException("Missing bearer token");
        var claims = jwtUtil.parse(header.substring(7))
                .orElseThrow(() -> new MessageDeliveryException("Invalid token"));
        var clinicId = claims.get("clinicId", Long.class);
        TenantContext.setCurrentTenant("clinic_" + clinicId);
        try {
            if (!activeUserChecker.isActive(Long.valueOf(claims.getSubject()), claims.get("role", String.class)))
                throw new MessageDeliveryException("Invalid token");
        } finally {
            TenantContext.clear();
        }
        if (attrs == null)
            throw new MessageDeliveryException("Invalid token");
        attrs.put("clinicId", clinicId);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class)));
        accessor.setUser(
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities)
        );
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        var attrs = accessor.getSessionAttributes();
        if (accessor.getUser() == null || attrs == null || !(attrs.get("clinicId") instanceof Long clinicId))
            throw new MessageDeliveryException("Not authenticated");
        if (!AppointmentChangedEvent.boardTopic(clinicId).equals(accessor.getDestination()))
            throw new MessageDeliveryException("Forbidden destination");
    }
}
