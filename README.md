# pulse-chat 💬

A real-time chat backend built with **Spring Boot 3**, **STOMP over WebSocket**, **PostgreSQL**, and **Redis**.

Clients connect over WebSocket, send chat messages that are persisted to Postgres and fanned out to every app instance through Redis pub/sub — so the service scales horizontally behind a load balancer. Typing indicators stay ephemeral, and presence ("who's online") is tracked in Redis with expiring keys.

## Features

- **STOMP over WebSocket** at `/ws` (SockJS fallback enabled)
  - `SEND /app/chat.send` → message persisted, broadcast to `/topic/rooms/{roomId}`
  - `SEND /app/chat.typing` → ephemeral typing indicator → `/topic/rooms/{roomId}/typing`
  - `SEND /app/chat.heartbeat` → refreshes presence (clients send ~every 30s)
- **Redis pub/sub fan-out**: every message is published on the `chat` channel; each app instance forwards it to its own connected clients
- **Presence**: `presence:{roomId}` Redis sets with 60s TTL — no explicit "leave" needed, stale entries expire
- **REST API** for room management, message history (paginated, newest first), and presence lookup
- **Bean Validation** on REST and STOMP payloads; consistent JSON error responses via `@RestControllerAdvice`
- **Unit tests** with Mockito — fast, no containers required

## Tech stack

| Layer      | Choice                                          |
|------------|-------------------------------------------------|
| Language   | Java 17                                         |
| Framework  | Spring Boot 3.3 (Web, WebSocket, Data JPA, Data Redis, Validation) |
| Database   | PostgreSQL 16                                   |
| Cache / MQ | Redis 7 (pub/sub + presence sets)               |
| Build      | Maven                                           |
| Misc       | Lombok, Jackson, SockJS                         |

## Architecture

```
 ┌────────┐   STOMP/ws    ┌────────────────────┐   JPA    ┌───────────┐
 │ client ├──────────────►│  pulse-chat (N x)  ├─────────►│ Postgres  │
 └────────┘               │                    │          │ rooms,    │
 ┌────────┐   /app/*      │  @MessageMapping   │◄─────────│ messages  │
 │ client ├──────────────►│  handlers          │  pub/sub └───────────┘
 └────────┘               │         │          │
                          │         ▼          │   Redis
                          │   "chat" channel   ├─────────►│ presence:{room}
                          │         │          │   sets
                          │         ▼          │
                          │  /topic/rooms/* ───┼─────────► STOMP clients
                          └────────────────────┘   (each instance
                                                    serves its own)
```

Message flow for `SEND /app/chat.send {roomId, content}`:

1. `ChatWebSocketController` resolves the sender from the WebSocket session (set at handshake time).
2. `MessageService` validates, persists the `Message` to Postgres, and publishes a `ChatMessageEvent` on Redis channel `chat`.
3. `ChatRedisListener` on **every** instance receives the event and pushes it via `SimpMessagingTemplate` to `/topic/rooms/{roomId}` for its locally connected clients.

With one instance this is a pass-through; with N instances behind a load balancer it's the fan-out that keeps chat working regardless of which instance a client landed on.

## API reference

### WebSocket destinations

Connect: `ws://localhost:8083/ws?username=alice` (SockJS endpoint; `username` query param is required by the demo handshake interceptor).

| Direction | Destination                        | Payload                        | Notes                                    |
|-----------|------------------------------------|--------------------------------|------------------------------------------|
| → server  | `/app/chat.send`                   | `{roomId, content}`            | persisted + broadcast                    |
| → server  | `/app/chat.typing`                 | `{roomId}`                     | ephemeral, not persisted                 |
| → server  | `/app/chat.heartbeat`              | `{roomId}`                     | refreshes presence TTL                   |
| ← server  | `/topic/rooms/{roomId}`            | `MessageResponse`              | new chat messages                        |
| ← server  | `/topic/rooms/{roomId}/typing`     | `TypingEvent`                  | typing indicators                        |
| ← server  | `/user/queue/errors`               | `ErrorResponse`                | per-sender STOMP error feedback          |

### REST endpoints

| Method | Path                              | Body / params              | Response              |
|--------|-----------------------------------|----------------------------|-----------------------|
| POST   | `/api/rooms`                      | `{name}`                   | `201` + `ChatRoomResponse` |
| GET    | `/api/rooms`                      | —                          | `[ChatRoomResponse]`  |
| GET    | `/api/rooms/{id}/messages`        | `?page=0&size=20`          | `Page<MessageResponse>` (newest first) |
| GET    | `/api/rooms/{id}/presence`        | —                          | `["alice","bob"]`     |
| DELETE | `/api/rooms/{id}`                 | —                          | `204` (also clears messages + presence) |

Errors always look like:

```json
{
  "timestamp": "2026-09-30T04:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Chat room not found: 99",
  "path": "/api/rooms/99/messages"
}
```

## Run it

**1. Start Postgres + Redis:**

```bash
docker compose up -d
```

**2. Run the app:**

```bash
mvn spring-boot:run
# app starts on http://localhost:8083
```

**3. Create a room and open the test client:**

```bash
curl -X POST localhost:8083/api/rooms \
  -H 'Content-Type: application/json' \
  -d '{"name":"general"}'
```

Then open **http://localhost:8083/chat-test.html** in two browser tabs (different usernames), connect, and chat. The page auto-sends heartbeats every 30s; hit **Presence** to see who's online. Typing indicators appear in the other tab.

**Run tests** (no Docker needed):

```bash
mvn test
```

## Design decisions & trade-offs

- **STOMP over raw WebSocket.** Raw WebSocket is just a byte pipe — you'd reinvent framing, routing, and subscriptions. STOMP gives destinations (`/app`, `/topic`), which map cleanly onto controllers and the broker, and SockJS adds fallback transports for restrictive networks/proxies.
- **Redis pub/sub for horizontal scaling.** The in-memory STOMP broker only knows about clients connected to *that* JVM. Publishing each message on a Redis channel and having every instance re-broadcast is the standard trick for scaling WebSocket apps without sticky sessions.
- **Persistence before publish.** The message is saved to Postgres *before* being published, so the durable record always exists even if broadcast fails. The remaining gap — publish succeeding but the DB transaction rolling back (or vice versa) — is the classic dual-write problem; production would use a transactional outbox table + relay instead.
- **Presence as a TTL set, not explicit join/leave.** Clients heartbeat; entries expire after 60s of silence. Simple and self-healing (no ghost users after a dropped connection). Trade-off: the TTL is per *key*, not per *member* — one active user keeps everyone's entry alive. A ZSET of `username → last-seen timestamp` with periodic pruning would be the precise version.
- **Typing indicators skip Redis.** They're ephemeral UI hints; broadcasting them only to clients on the same instance is an accepted simplification. Fanning them out too would be a one-line change (`convertAndSend` → publish on a `typing` channel).
- **Demo handshake auth.** `?username=` in the handshake URL is trivially spoofable — it's here so the project runs without an auth server. Production: validate a JWT in the STOMP CONNECT frame via a `ChannelInterceptor` and reject unauthenticated CONNECTs.

## What I'd add next

- JWT auth on the CONNECT frame (see above)
- Cursor-based pagination for message history (offset paging degrades on large rooms)
- Message edit/delete with broadcast of updates
- Rate limiting per user (e.g. Redis token bucket)
- Moderation hooks (profanity filter / report flow)
- Private/direct-message rooms
