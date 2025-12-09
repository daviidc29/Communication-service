
# UpLearn – Chat Service (WebSocket + Redis + MongoDB)

[![Java](https://img.shields.io/badge/Java-17-007396?logo=java)]() [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-6DB33F?logo=springboot)]() [![MongoDB](https://img.shields.io/badge/MongoDB-Atlas%20%2F%20Local-47A248?logo=mongodb)]() [![Redis](https://img.shields.io/badge/Redis-Optional-D82C20?logo=redis)]() [![WebSocket](https://img.shields.io/badge/WebSocket-Native-1f6feb)]()

Microservicio de **mensajería instantánea** para el ecosistema UpLearn.
Provee **REST** para utilidades del chat e historial, y **WebSocket nativo** para mensajería en tiempo real, con **MongoDB** para persistencia y **Redis Pub/Sub** (opcional) para escalar horizontalmente.

> **Conecta así:** `ws://{HOST}:{PORT}/ws/chat?token=JWT`
> Envías JSON `{ "toUserId": "...", "content": "..." }` y recibes mensajes con forma **ChatMessageData**.

---

## ✨ Características

* **WebSocket nativo** (Spring) con validación por **JWT** (query `?token=`).
* **Persistencia** de chats y mensajes en **MongoDB**.
* Entrega de **pendientes**: si el receptor está offline, se marcan `delivered=false` y se envían al reconectar.
* **Redis Pub/Sub (opcional)** para difundir mensajes entre réplicas (`chat:*`).
* **CORS** configurable y **caché** (Caffeine) para **roles** y **perfiles** públicos de usuario.
* Integración con microservicios:

  * **Users**: roles y perfil público.
  * **Reservations**: autoriza si dos usuarios **pueden chatear** (regla de negocio).

---

## 🧩 Arquitectura (alto nivel)

```
Frontend (React) ──HTTP──► REST (ChatController) ──► MongoDB
     │                                  │
     └──WS ?token=JWT──► ChatWebSocketGateway ──► Redis Pub/Sub (opcional)
                                         │
                                  AuthorizationService / ReservationClient / UserServiceClient
```

---

## 🛣️ Endpoints REST

| Método | Ruta                                   | Descripción                                                         |
| ------ | -------------------------------------- | ------------------------------------------------------------------- |
| GET    | `/`                                    | Health del servicio (`service=status`).                             |
| GET    | `/api/chat/contacts`                   | Lista de **contactos** (derivados de reservas válidas e historial). |
| GET    | `/api/chat/history/{chatId}`           | **Historial** del chat (orden cronológico).                         |
| GET    | `/api/chat/chat-id/with/{otherUserId}` | Utilidad para calcular/obtener `chatId` entre dos usuarios.         |

> Todos los `/api/**` requieren `Authorization: Bearer <JWT>` (filtrados por `AuthFilter`).

---

## 🔌 WebSocket en 60 segundos

**URL:** `/ws/chat?token=<JWT>` (query param).
**Handshake:** el gateway extrae el `userId` del token y registra la sesión.

**Enviar mensaje (cliente → servidor)**

```json
{
  "toUserId": "uuid-del-receptor",
  "content": "Hola 👋"
}
```

**Recibir mensaje (servidor → cliente) – ChatMessageData**

```json
{
  "id": "msgId",
  "chatId": "chat:userA:userB",
  "fromUserId": "uuid-emisor",
  "toUserId": "uuid-receptor",
  "content": "Hola 👋",
  "createdAt": "2025-12-09T22:05:00Z",
  "delivered": true,
  "read": false
}
```

**Notas de entrega**

* Si el receptor está **online en este nodo**, se entrega directo.
* Si está online en **otro nodo**, se **publica** el payload en Redis (`chat:*`) y ese nodo lo entrega.
* Si está **offline**, el mensaje queda almacenado en Mongo con `delivered=false`; al reconectar se vacían los pendientes.

### Snippet mínimo (JS cliente)

```js
const ws = new WebSocket(`wss://host/ws/chat?token=${jwt}`);
ws.onmessage = (ev) => console.log('Mensaje', JSON.parse(ev.data));
ws.onopen = () => ws.send(JSON.stringify({ toUserId, content: 'Hola!' }));
```

---

## ⚙️ Configuración

Crea un `.env` (o variables de entorno equivalentes):

```properties
# Puerto
PORT=8091

# Mongo
DB_URI=mongodb+srv://user:pass@cluster/db
DB_NAME=chatdb

# Redis (opcional; si se omite, funciona en single-instance)
REDIS_ENABLED=false
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL=false

# Integraciones
RESERVATIONS_API_BASE=http://localhost:8090     # para can-chat y contactos
USERS_API_BASE=http://localhost:8080            # para roles
USERS_PUBLIC_PATH=/Api-user/public              # para perfíl público

# Cache (Caffeine)
ROLES_CACHE_TTL=300
ROLES_CACHE_MAX=500
PROFILES_CACHE_TTL=300
PROFILES_CACHE_MAX=1000

# Crypto (para utilidades internas)
CHAT_CRYPTO_SECRET=elige-una-clave-larga

# CORS
APP_CORS_ALLOWED=http://localhost:3000
```

> El archivo `application.properties` ya hace `spring.config.import=optional:file:.env[.properties]`.

---

## 🚀 Ejecución local

**Con Redis (opcional):**

```bash
docker run --name redis -p 6379:6379 -d redis:7
```

**Backend:**

```bash
mvn clean package
mvn spring-boot:run
# o: java -jar target/chat-service-1.0.0.jar
```

---

## 🧪 Pruebas y cobertura

* JUnit 5 + Mockito, JaCoCo, regla mínima `INSTRUCTION` 80% (configurada en `pom.xml`).
* Tests sobre config (CORS, Redis, WS), controladores y servicios clave.

---

## 🔍 Modelo de datos (Mongo)

**Chat**

```json
{
  "id": "chatId",
  "userA": "uuid",
  "userB": "uuid",
  "createdAt": "2025-12-09T22:00:00Z",
  "participants": ["uuidA","uuidB"]
}
```

**Message**

```json
{
  "id": "msgId",
  "chatId": "chatId",
  "fromUserId": "uuid",
  "toUserId": "uuid",
  "content": "texto",
  "createdAt": "2025-12-09T22:05:00Z",
  "delivered": true,
  "read": false
}
```

---

## 🔐 Seguridad y autorización (resumen)

* **JWT** requerido para REST (`Authorization: Bearer ...`) y para WS (query `?token=`).
* Antes de **enviar** un mensaje, el gateway valida con **Reservations** (`canChat`) que **exista permiso** entre emisor y receptor (por ejemplo, reserva aceptada).
* **CORS** con `allowedOriginPatterns`.

---

## 🧭 Mapa rápido del código

```
src/main/java/co/.../chat/
├─ ChatServiceApplication.java        # Main, @SpringBootApplication, @EnableCaching
├─ config/
│  ├─ WebConfig.java                  # CORS
│  ├─ WebSocketConfig.java            # Registra /ws/chat
│  ├─ AuthFilter.java                 # Filtro para /api/** (JWT + CORS aware errors)
│  ├─ RedisConfig.java                # Beans Lettuce/Redis si redis.enabled=true
│  └─ CacheConfig.java                # Caffeine caches para roles/perfiles
├─ controller/
│  ├─ RootController.java             # GET /
│  └─ ChatController.java             # /api/chat/contacts, /history/{id}, /chat-id/with/{uid}
├─ ws/
│  └─ ChatWebSocketGateway.java       # TextWebSocketHandler (WS nativo)
├─ service/
│  ├─ AuthorizationService.java       # Decodifica JWT, subject(), requireRole(), me()
│  ├─ ChatService.java                # chatIdOf, saveMessage, pending, markDelivered, toDto
│  ├─ ReservationClient.java          # canChat(), counterpartIds()
│  └─ UserServiceClient.java          # roles + perfil público con caché
├─ repository/
│  ├─ ChatRepository.java             # MongoRepository<Chat>
│  └─ MessageRepository.java          # MongoRepository<Message> + queries
├─ domain/
│  ├─ Chat.java                       # Documento chat
│  └─ Message.java                    # Documento mensaje
└─ dto/
   ├─ ChatMessageData.java            # DTO para emitir por WS/REST
   ├─ SendMessageRequest.java         # Payload entrante WS
   ├─ ChatContact.java / PublicProfile.java / RolesResponse.java
```

---

## 🧯 Errores comunes

* **401/403**: Token ausente o inválido (REST: header; WS: `?token=`).
* **No entrega en clúster**: revisa `REDIS_ENABLED=true` y variables Redis.
* **CORS**: ajusta `APP_CORS_ALLOWED` (dominios separados por coma).

---

## ❓¿Usa STOMP?

> **No.** Este servicio usa **WebSocket nativo** con `TextWebSocketHandler`.
> No hay `SimpMessagingTemplate` ni destinos `/topic`/`/queue` de STOMP.
> La mensajería se gestiona con JSON directo y, para distribución, **Redis Pub/Sub** (`chat:*`).

> **¿Y si quisiera STOMP?** Se podría añadir `spring-messaging`, configurar un `MessageBroker` simple y usar endpoints tipo `/app/send` y `/topic/chat.{id}`; no está implementado porque el diseño actual es más **ligero** y encaja con la lógica de autorización por reserva previa.

---
