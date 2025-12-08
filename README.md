# UpLearn Chat Service (WebSocket + Redis + Mongo)

Microservicio de mensajería instantánea para UpLearn. Compatible con Java 17 y Spring Boot 3.3.x.
Integra seguridad y resolución de roles de la misma manera que *student-tutor-scheduler*:
- Importa `.env` para `DB_URI` y `DB_NAME`
- Resuelve roles llamando al servicio de usuarios en `http://localhost:8080/Api-user`
- Valida permiso de chat con el servicio de reservas en `http://localhost:8090/api/reservations/can-chat?withUserId=...`

## Endpoints

- `GET /api/chat/contacts` → contactos (derivados de historial/pedientes; se puede extender)
- `GET /api/chat/history/{chatId}` → historial del chat
- `GET /api/chat/chat-id/with/{otherUserId}` → utilitario para calcular `chatId`

## WebSocket

- URL: `ws://localhost:8091/ws/chat?token=<ID_TOKEN>`
- Mensaje de salida (JSON):
  ```json
  { "toUserId": "abc123", "content": "Hola!" }
  ```
- Broadcast: el servidor guarda en Mongo y publica en Redis `chat:{chatId}`;
  todos los nodos reciben y entregan a las sesiones (del emisor y del receptor).
- Si el receptor está **offline**, el mensaje queda con `delivered=false` y se envía al reconectar.


## Levantar
1. Abrir docker desktop en el computador y esperar que inicie
2. Dentro de la carpeta raiz del proyecto poner:
```
docker run --name redis -p 6379:6379 -d redis:7
mvn spring-boot:run
```

---
