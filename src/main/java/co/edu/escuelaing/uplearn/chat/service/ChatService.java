package co.edu.escuelaing.uplearn.chat.service;

import co.edu.escuelaing.uplearn.chat.domain.Chat;
import co.edu.escuelaing.uplearn.chat.domain.Message;
import co.edu.escuelaing.uplearn.chat.dto.ChatMessageData;
import co.edu.escuelaing.uplearn.chat.repository.ChatRepository;
import co.edu.escuelaing.uplearn.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.domain.Sort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * Servicio para manejar la lógica de chats y mensajes.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chats;
    private final MessageRepository messages;
    private final CryptoService crypto;
    private final MongoTemplate mongo;

    /**
     * Generar un chatId único y consistente para dos usuarios
     * 
     * @param a el ID del primer usuario
     * @param b el ID del segundo usuario
     * @return el chatId generado
     */
    public String chatIdOf(String a, String b) {
        String userA = a.compareTo(b) <= 0 ? a : b;
        String userB = a.compareTo(b) <= 0 ? b : a;
        String key = userA + ":" + userB;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte d : digest) {
                sb.append(String.format("%02x", d));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 MessageDigest algorithm not available", e);
        }
    }

    /**
     * Asegurar que un chat entre dos usuarios existe, creándolo si es necesario
     * 
     * @param a el ID del primer usuario
     * @param b el ID del segundo usuario
     * @return la entidad Chat existente o recién creada
     */
    public Chat ensureChat(String a, String b) {
        String userA = a.compareTo(b) <= 0 ? a : b;
        String userB = a.compareTo(b) <= 0 ? b : a;
        return chats.findByUserAAndUserB(userA, userB)
                .orElseGet(() -> chats.save(Chat.builder()
                        .id(chatIdOf(a, b))
                        .userA(userA)
                        .userB(userB)
                        .participants(Set.of(userA, userB))
                        .createdAt(Instant.now())
                        .build()));
    }

    /**
     * Guardar un mensaje en un chat (contenido cifrado en BD)
     * 
     * @param chatId  el ID del chat
     * @param from    el ID del usuario remitente
     * @param to      el ID del usuario destinatario
     * @param content el contenido del mensaje
     * @return la entidad Message guardada
     */
    public Message saveMessage(String chatId, String from, String to, String content) {
        String encrypted = crypto.encrypt(content);
        Message msg = Message.builder()
                .chatId(chatId)
                .fromUserId(from)
                .toUserId(to)
                .content(encrypted)
                .createdAt(Instant.now())
                .delivered(false)
                .read(false)
                .build();
        return messages.save(msg);
    }

    /**
     * Obtener el historial de mensajes de un chat (entidades)
     * 
     * @param chatId ID del chat
     * @return Lista de mensajes del chat
     */
    public List<Message> history(String chatId) {
        return messages.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    /**
     * Obtener los mensajes pendientes de entrega para un usuario
     * 
     * @param userId ID del usuario
     * @return Lista de mensajes pendientes de entrega
     */
    public List<Message> pendingFor(String userId) {
        return messages.findByToUserIdAndDeliveredIsFalseOrderByCreatedAtAsc(userId);
    }

    /**
     * Marcar una lista de mensajes como entregados
     * 
     * @param list la lista de mensajes a marcar
     */
    public void markDelivered(List<Message> list) {
        list.forEach(m -> m.setDelivered(true));
        messages.saveAll(list);
    }

    /**
     * Convertir una entidad Message a DTO ChatMessageData (desencripta y es
     * null-safe)
     * 
     * @param m la entidad Message
     * @return el DTO ChatMessageData correspondiente
     */
    public ChatMessageData toDto(Message m) {
        Instant ts = m.getCreatedAt();
        String created = (ts != null ? ts.toString() : Instant.now().toString());
        String plainContent = crypto.decrypt(m.getContent());

        return ChatMessageData.builder()
                .id(m.getId())
                .chatId(m.getChatId())
                .fromUserId(m.getFromUserId())
                .toUserId(m.getToUserId())
                .content(plainContent)
                .createdAt(created)
                .delivered(m.isDelivered())
                .read(m.isRead())
                .build();
    }

    /**
     * Lectura tolerante a esquema para historial (sin mapear a entidad)
     * 
     * @param chatId ID del chat
     * @return Lista de mensajes del chat como DTO ChatMessageData
     */
    public List<ChatMessageData> historyDtoTolerant(String chatId) {
        Query q = new Query(Criteria.where("chatId").is(chatId))
                .with(Sort.by(Sort.Direction.ASC, "createdAt"));
        List<org.bson.Document> docs = mongo.find(q, org.bson.Document.class, "messages");

        List<ChatMessageData> out = new ArrayList<>(docs.size());
        for (org.bson.Document d : docs) {
            String id = String.valueOf(d.getOrDefault("_id", ""));
            String from = asStr(d.get("fromUserId"));
            String to = asStr(d.get("toUserId"));
            String enc = asStr(d.get("content"));
            String content = crypto.decrypt(enc);

            String createdIso = toIso(d.get("createdAt"));
            boolean delivered = toBool(d.get("delivered"));
            boolean read = toBool(d.get("read"));

            out.add(ChatMessageData.builder()
                    .id(id)
                    .chatId(asStr(d.get("chatId")))
                    .fromUserId(from)
                    .toUserId(to)
                    .content(content)
                    .createdAt(createdIso)
                    .delivered(delivered)
                    .read(read)
                    .build());
        }
        return out;
    }

    /**
     * Convierte un valor a cadena de texto de forma segura
     * 
     * @param v el valor a convertir
     * @return el valor convertido a cadena
     */
    private static String asStr(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    /**
     * Convierte un valor a cadena ISO-8601 de forma segura
     * 
     * @param v el valor a convertir
     * @return el valor convertido a cadena ISO-8601
     */
    private static String toIso(Object v) {
        if (v instanceof java.util.Date dt)
            return dt.toInstant().toString();
        if (v instanceof org.bson.types.ObjectId oid)
            return Instant.ofEpochMilli(oid.getTimestamp() * 1000L).toString();
        if (v instanceof String s) {
            try {
                return Instant.parse(s).toString();
            } catch (Exception ignored) {
                /* ignore: not a valid ISO-8601 instant */ }
            try {
                return ZonedDateTime.parse(s).toInstant().toString();
            } catch (Exception ignored) {
                /* ignore: not a valid zoned-date-time */ }
            try {
                return Instant.ofEpochMilli(Long.parseLong(s)).toString();
            } catch (Exception ignored) {
                /* ignore: not a milliseconds epoch string */ }
            return s;
        }
        return Instant.now().toString();
    }

    /**
     * Convierte un valor a booleano de forma segura
     * 
     * @param v el valor a convertir
     * @return el valor convertido a booleano
     */
    private static boolean toBool(Object v) {
        if (v instanceof Boolean b)
            return b;
        if (v instanceof Number n)
            return n.intValue() != 0;
        if (v instanceof String s)
            return "true".equalsIgnoreCase(s) || "1".equals(s);
        return false;
    }
}