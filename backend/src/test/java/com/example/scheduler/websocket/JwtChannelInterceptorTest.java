package com.example.scheduler.websocket;

import com.example.scheduler.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtChannelInterceptorTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-secret-test-secret-test-secret-0123456789");
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtUtil);

    @Test
    void connect_should_reject_when_tokenMissing() {
        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connect_should_reject_when_tokenInvalid() {
        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.addNativeHeader("Authorization", "Bearer not-a-jwt");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connect_should_setPrincipalAndClinic_when_tokenValid() {
        var accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());
        accessor.addNativeHeader("Authorization", "Bearer " + jwtUtil.generate(42L, "ASSISTANT", 3L, "ana"));
        accessor.setLeaveMutable(true);

        var result = interceptor.preSend(message(accessor), null);

        var after = StompHeaderAccessor.wrap(result);
        assertThat(after.getUser()).isNotNull();
        assertThat(after.getUser().getName()).isEqualTo("42");
        assertThat(after.getSessionAttributes()).containsEntry("clinicId", 3L);
    }

    @Test
    void subscribe_should_reject_when_otherClinicTopic() {
        var accessor = subscribe("/topic/clinic/4/board");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribe_should_reject_when_unknownDestination() {
        var accessor = subscribe("/topic/clinic/3/other");

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribe_should_accept_when_ownClinicBoard() {
        var accessor = subscribe("/topic/clinic/3/board");

        assertThat(interceptor.preSend(message(accessor), null)).isNotNull();
    }

    private static StompHeaderAccessor subscribe(String destination) {
        var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionAttributes(new HashMap<>(Map.of("clinicId", 3L)));
        accessor.setUser(new UsernamePasswordAuthenticationToken("42", null));
        accessor.setDestination(destination);
        return accessor;
    }

    private static Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
