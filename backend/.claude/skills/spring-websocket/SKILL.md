---
name: spring-websocket
description: 'WebSockets with STOMP in Spring Boot for real-time notifications: WebSocketConfig with simple broker and heartbeats, JWT authentication on the CONNECT frame with a ChannelInterceptor, SUBSCRIBE authorization per topic (tenant isolation), and publishing events after commit with @TransactionalEventListener. Use when adding live updates, pushing to the frontend, per-tenant/per-user topics, or debugging WS connections.'
---

# WebSocket (STOMP) server → client

Pattern: the server **only pushes** lightweight events (id + status); the client re-fetches via REST if it needs details. Clients do not send messages (`SEND` is blocked). One topic per tenant: `/topic/tenant/{tenantId}/<channel>`.

```
Service (@Transactional) ── publishEvent(XChangedEvent) ──► [commit] ──► XPublisher (@TransactionalEventListener AFTER_COMMIT)
                                                                           └─ SimpMessagingTemplate.convertAndSend(topic, payload)
Client ── CONNECT (Authorization: Bearer) ──► JwtChannelInterceptor: validates JWT + active user, stores tenantId in the session
       ── SUBSCRIBE /topic/tenant/{id}/board ──► Interceptor: only its own topic
```

## 1. Dependency

```groovy
implementation 'org.springframework.boot:spring-boot-starter-websocket'
```

## 2. WebSocketConfig

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);   // + .withSockJS() if the client uses SockJS
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10_000, 10_000})     // detects dead clients
                .setTaskScheduler(heartbeatScheduler());           // required when heartbeats are on
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }

    @Bean
    public ThreadPoolTaskScheduler heartbeatScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }
}
```
In `SecurityConfig`: `a.requestMatchers("/ws/**").permitAll()`. The browser's HTTP handshake carries no Authorization header; authentication happens on the STOMP `CONNECT` frame.

## 3. Events

```java
public record BoardEvent(Long itemId, ItemStatus status) {}       // payload sent to the client

public record ItemChangedEvent(Long tenantId, BoardEvent payload) {
    public static String boardTopic(Long tenantId) {               // single source of truth for the topic
        return "/topic/tenant/" + tenantId + "/board";
    }
}
```

## 4. Publisher (after commit)

```java
@Component
@RequiredArgsConstructor
public class BoardPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)   // never notify changes that rolled back
    public void onItemChanged(ItemChangedEvent event) {
        messagingTemplate.convertAndSend(ItemChangedEvent.boardTopic(event.tenantId()), event.payload());
    }
}
```
In the service, centralize it in a helper:
```java
private Item saveAndNotify(Item item) {
    var saved = itemRepository.save(item);
    var tenantId = Long.valueOf(TenantContext.getCurrentTenant().substring("tenant_".length()));
    eventPublisher.publishEvent(new ItemChangedEvent(tenantId, new BoardEvent(saved.getId(), saved.getStatus())));
    return saved;
}
```
The `tenantId` is captured **inside** the transaction (in `AFTER_COMMIT` the `TenantContext` may no longer be reliable). If the method is not `@Transactional`, the listener never fires: use `@TransactionalEventListener(fallbackExecution = true)` or guarantee the transaction.

## 5. JwtChannelInterceptor

```java
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
                .orElseThrow(() -> new MessageDeliveryException("Invalid token"));   // reject on CONNECT, don't wait for SUBSCRIBE
        var tenantId = claims.get("tenantId", Long.class);
        TenantContext.setCurrentTenant("tenant_" + tenantId);      // the active-user check queries the tenant schema
        try {
            if (!activeUserChecker.isActive(Long.valueOf(claims.getSubject()), claims.get("role", String.class)))
                throw new MessageDeliveryException("Invalid token");
        } finally {
            TenantContext.clear();
        }
        if (attrs == null) throw new MessageDeliveryException("Invalid token");
        attrs.put("tenantId", tenantId);                           // persists for the whole WS session
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class)));
        accessor.setUser(new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        var attrs = accessor.getSessionAttributes();
        if (accessor.getUser() == null || attrs == null || !(attrs.get("tenantId") instanceof Long tenantId))
            throw new MessageDeliveryException("Not authenticated");
        if (!ItemChangedEvent.boardTopic(tenantId).equals(accessor.getDestination()))
            throw new MessageDeliveryException("Forbidden destination");   // nobody subscribes to another tenant's topic
    }
}
```
Throwing `MessageDeliveryException` in the interceptor makes Spring send an `ERROR` frame and close the session.

Several allowed topics: replace the `equals` with `Set.of(boardTopic(id), otherTopic(id)).contains(dest)`. Per-user messages: `convertAndSendToUser(userId, "/queue/notifications", payload)` + subscribe to `/user/queue/notifications` (the `Principal` set on CONNECT is the target).

## 6. Client (reference, @stomp/stompjs)

```ts
const client = new Client({
  brokerURL: 'ws://host/ws',
  connectHeaders: { Authorization: `Bearer ${token}` },
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  reconnectDelay: 5000,
  onConnect: () => client.subscribe(`/topic/tenant/${tenantId}/board`, msg => onEvent(JSON.parse(msg.body))),
});
client.activate();
```

## Scaling

The simple broker lives in a single instance's memory. With several replicas, switch to `enableStompBrokerRelay(...)` (RabbitMQ/ActiveMQ) without touching the publisher or the interceptor.
