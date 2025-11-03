# CollabDoc: A Real-Time Collaborative Document Editor

CollabDoc is a high-performance, real-time collaborative document editor built with a Java Spring Boot backend and a React frontend. This project is designed to demonstrate advanced backend engineering concepts, including real-time data synchronization with WebSockets, efficient persistence using a "write-behind" cache with Redis, and a secure, role-based permission system.

This project goes beyond a standard CRUD application to tackle complex challenges like concurrency, real-time data flow, and system design for scalability.

## Features (v1.0)

- **Real-Time Collaborative Editing**: Multiple users can edit the same document simultaneously. Changes are broadcast to all other participants instantly via WebSockets (STOMP).
- **JWT Authentication & Security**: Secure REST API and WebSocket endpoints using Spring Security and JSON Web Tokens (JWT).
- **Document Management**: Full CRUD (Create, Read, Update, Delete, Share the doc ) functionality for documents.
- **Efficient Persistence (The "Dirty Flag" Pattern)**: To prevent database overload, live edits are written to a high-speed Redis cache. A background task (@Async @Scheduled) runs every 10 seconds to persist only the "dirty" documents (those that have changed) to the PostgreSQL database.
- **Role-Based Permissions**: Document Owners can share documents with other users as either VIEWER or EDITOR.
- **Robust Security**:
  - Backend logic prevents VIEWERs from sending edit messages.
  - API endpoints are secured based on ownership or share permissions.
- **Echo Prevention**: Client-side logic correctly handles broadcasted messages to prevent the user's own edits from being "echoed" back to them, solving the "double-typing" bug.

## Tech Stack

| Category | Technology | Purpose |
|-----------|-------------|----------|
| **Backend** | Java 21 / Spring Boot 3 | Core application framework, REST API, WebSocket server |
|  | Spring Security | JWT-based authentication and endpoint authorization |
|  | Spring Data JPA (Hibernate) | ORM for database interaction (PostgreSQL) |
| **Frontend** | React (with Hooks) | Modern, component-based user interface |
|  | Quill.js | Rich text editor component |
|  | @stomp/stompjs & SockJS | Client-side WebSocket & STOMP connection |
|  | Axios | HTTP client for REST API communication |
| **Database** | PostgreSQL | Primary relational database for persistent storage (users, docs, permissions) |
| **Cache & Real-Time** | Redis | High-speed in-memory cache for live document content & the "dirty flag" set |
|  | Spring WebSockets (STOMP) | Real-time, bidirectional message broker for live edits |

## v2 Roadmap (Future Scalability Plans)

The v1.0 build is a robust, durable, stateful monolith. The v2 roadmap addresses the key bottlenecks for scaling to millions of users, based on advanced system design principles.

### 1. Stateless Servers (Horizontal Scaling)
**Problem:** The current WebSocket broker is stateful (running in the app's memory). This means we cannot scale out by adding more servers, as users on different servers wouldn't see each other's edits.  
**v2 Solution:** Externalize the message broker using RabbitMQ (or Redis Pub/Sub). This makes the application servers stateless. An edit sent to Server A will be published to a RabbitMQ topic, which then broadcasts it to all other servers (B, C, D...) to be sent to their connected clients.

### 2. Advanced Echo Prevention
**Problem:** Our "echo fix" (ignoring messages from our own username) prevents a user from seeing their own edits in another browser tab.  
**v2 Solution:** Tag messages with a unique `sessionId` instead of username. This allows the client to ignore echoes from the specific tab that sent the edit, while still receiving updates on all other tabs.

### 3. Conflict-Free Resolution (OT/CRDT)
**Problem:** Our current design has a race condition. If two users type at the exact same index simultaneously, the "last write wins" and one edit is lost.  
**v2 Solution:** Implement Operational Transformation (OT) (like Google Docs) or a CRDT to mathematically merge conflicting deltas, ensuring no data is ever lost.

### 4. Efficient Network Payloads
**Problem:** The client currently sends the delta (the change) and the fullDelta (the whole document) on every edit, which is not efficient.  
**v2 Solution:** The client will send only the delta. The backend service will fetch the current document from Redis and apply the delta itself before saving, trading server-side complexity for major network efficiency gains.
