package co.edu.escuelaing.uplearn.chat.controller;

import co.edu.escuelaing.uplearn.chat.domain.Message;
import co.edu.escuelaing.uplearn.chat.dto.ChatContact;
import co.edu.escuelaing.uplearn.chat.dto.ChatMessageData;
import co.edu.escuelaing.uplearn.chat.dto.PublicProfile;
import co.edu.escuelaing.uplearn.chat.service.AuthorizationService;
import co.edu.escuelaing.uplearn.chat.service.ChatService;
import co.edu.escuelaing.uplearn.chat.service.ReservationClient;
import co.edu.escuelaing.uplearn.chat.service.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controlador REST para la funcionalidad de chat.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String UNKNOWN_MESSAGE_ID = "<unknown>";

    private final AuthorizationService authz;
    private final ChatService chat;
    private final ReservationClient reservations;
    private final UserServiceClient users;

    /**
     * Lista de contactos con los que el usuario autenticado tiene reservas válidas.
     *
     * @return Lista de contactos de chat
     */
    @GetMapping("/contacts")
    public List<ChatContact> contacts(@RequestHeader("Authorization") String authorization) {
        var me = authz.me(authorization);
        String myId = me.getId();

        Set<String> ids = reservations.counterpartIds(authorization, myId);

        return ids.stream().map(id -> {
            PublicProfile p = users.getPublicProfileById(id);
            if (p == null) {
                p = PublicProfile.builder().id(id).name("Usuario").email("").build();
            }
            return ChatContact.builder()
                    .id(p.getId())
                    .sub(p.getSub())
                    .name(p.getName())
                    .email(p.getEmail())
                    .avatarUrl(p.getAvatarUrl())
                    .build();
        }).toList();
    }

    /**
     * Historial de mensajes para un chat concreto.
     * Se intenta mapear cada mensaje; si alguno falla, se loguea y se omite.
     *
     * @param chatId ID del chat
     * @return Lista de mensajes del chat o un error en caso de fallo
     */
    @GetMapping("/history/{chatId}")
    public ResponseEntity<Object> history(
            @PathVariable("chatId") String chatId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        try {
            List<Message> raw = chat.history(chatId);
            if (raw == null) {
                return ResponseEntity.ok(List.of());
            }

            List<ChatMessageData> out = new ArrayList<>(raw.size());
            for (Message m : raw) {
                addMessageIfConvertible(out, m, chatId);
            }
            return ResponseEntity.ok(out);

        } catch (Exception e) {
            log.error("Error cargando historial para chat {}: {}", chatId, e.toString(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Error cargando historial del chat",
                            "details", e.getMessage()));
        }
    }

    /**
     * Intenta convertir un Message a ChatMessageData y añadirlo a la lista destino.
     * En caso de error, se loguea y se continúa sin romper el flujo.
     *
     * @param target Lista destino donde agregar el DTO
     * @param message Mensaje de dominio a convertir
     * @param chatId ID del chat (para logging)
     */
    private void addMessageIfConvertible(List<ChatMessageData> target, Message message, String chatId) {
        try {
            target.add(chat.toDto(message));
        } catch (Exception ex) {
            String mid = safeMessageId(message);
            log.error("Error convirtiendo mensaje {} de chat {}: {}", mid, chatId, ex.toString(), ex);
        }
    }

    /**
     * Obtener un ID de mensaje seguro para logging.
     *
     * @param m el mensaje del cual obtener el ID
     * @return ID del mensaje o "<unknown>" si no está disponible
     */
    private String safeMessageId(Message m) {
        try {
            if (m != null && m.getId() != null) {
                return m.getId();
            }
        } catch (Exception ex) {
            log.debug("Ignored error obtaining message id", ex);
        }
        return UNKNOWN_MESSAGE_ID;
    }

    /**
     * Calcula el chatId entre el usuario autenticado y otro usuario.
     *
     * @param otherUserId   ID del otro usuario
     * @param authorization encabezado de autorización del usuario autenticado
     * @return Mapa con el chatId y el ID del usuario autenticado
     */
    @GetMapping("/chat-id/with/{otherUserId}")
    public Map<String, String> chatId(
            @PathVariable("otherUserId") String otherUserId,
            @RequestHeader("Authorization") String authorization) {

        String meId = authz.subject(authorization);
        String chatId = chat.chatIdOf(meId, otherUserId);

        return Map.of(
                "chatId", chatId,
                "meId", meId);
    }
}
