---
name: spring-websocket
description: STOMP over WebSocket configuration and realtime publishing in the scheduler backend. Use when adding or changing realtime features, new topics, websocket security, heartbeats, or when the frontend board needs to react to backend changes.
---

# Spring WebSocket (STOMP)

The backend pushes realtime updates to staff clients with STOMP over a plain WebSocket, using Spring's in-memory simple broker. Clients only subscribe; they never send application messages.

## Building blocks

| Class                             | Role                                                                                                                                                         |
|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `config.WebSocketConfig`          | Enables the message broker, registers the `/ws` endpoint, configures the simple broker (`/topic`, `/queue`), heartbeats and the inbound channel interceptor. |
| `websocket.JwtChannelInterceptor` | Authenticates on CONNECT, authorizes on SUBSCRIBE, rejects SEND.                                                                                             |
| `websocket.BoardPublisher`        | Listens to domain events after commit and sends payloads with `SimpMessagingTemplate`.                                                                       |
| `websocket.event.*`               | Records describing what is published (the internal event and the client payload) plus the topic naming helper.                                               |

## Endpoint and broker

- Single endpoint `/ws`, allowed origins taken from the same `cors.allowed-origins` property used by the REST CORS config. Keep both in sync.
- Simple broker prefixes `/topic` (broadcast) and `/queue` (per-user, reserved). Application prefix `/app` exists but inbound SEND is blocked by design.
- Heartbeats are 10 seconds in both directions and run on a dedicated single-thread scheduler bean. Any new scheduling need should not reuse that scheduler.
- `/ws/**` is a public HTTP path in security and is excluded from rate limiting; authentication happens at the STOMP level, not on the HTTP handshake.

## Security model

- CONNECT: the client sends the JWT in the `Authorization` native header. The interceptor parses it, rejects guests, sets the tenant temporarily to verify the user is still active and has the same role, stores the clinic id in the session attributes and sets the authenticated principal.
- SUBSCRIBE: allowed only if the session is authenticated and its token (expiry stored in the session on CONNECT) has not expired. Destinations: the clinic board `/topic/clinic/{clinicId}/board` for unrestricted staff, and the per-doctor board `/topic/clinic/{clinicId}/doctor/{doctorId}/board` (a restricted DOCTOR may only subscribe to their own; other staff to any doctor of their clinic). Any other destination is rejected. Expiry is only checked on SUBSCRIBE; existing subscriptions keep receiving events until disconnect (payloads are ids and statuses only). When adding a new topic, extend this authorization explicitly; never allow wildcard subscriptions.
- SEND: always rejected. If a feature truly needs client-to-server messages, prefer a REST endpoint.
- Always clear `TenantContext` in a finally block after any tenant-scoped work done inside the interceptor.

## Topic design

- Topics are tenant scoped: `/topic/clinic/{clinicId}/<feature>`. Build the string in one static helper on the event record so the publisher and the interceptor share the same source of truth.
- Payloads are small records with identifiers and the new state (for example appointment id and status). Clients refetch details through REST if they need more. Never push entities or personal data that the subscriber's role would not see through REST.

## Publishing rules

- Services never call `SimpMessagingTemplate` directly. They publish a domain event through `ApplicationEventPublisher` inside the transaction.
- The publisher listens with `@TransactionalEventListener(phase = AFTER_COMMIT)` so clients are notified only about committed data.
- Publishing failures are logged and swallowed; a broken websocket must never roll back or fail the business operation.
- One publisher class per realtime area; each handler method handles one event type.

## Adding a new realtime feature (checklist)

1. Define the client payload record and the internal event record (clinic id plus payload) in `websocket.event`, with a static topic helper.
2. Publish the event from the service method that changes the state.
3. Add a handler in a publisher component, after commit, with error logging.
4. Extend the SUBSCRIBE authorization in the interceptor for the new destination, scoped to the session's clinic and, if needed, the role.
5. On the frontend, consume it through the socket service in `core/api` (see the `angular-architecture` skill).
6. Test: unit test the interceptor decisions (CONNECT accepted and rejected, SUBSCRIBE allowed and forbidden, SEND rejected) and the publisher (sends to the right topic, swallows errors).
