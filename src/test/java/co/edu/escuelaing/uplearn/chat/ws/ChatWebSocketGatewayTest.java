package co.edu.escuelaing.uplearn.chat.ws;

import co.edu.escuelaing.uplearn.chat.domain.Message;
import co.edu.escuelaing.uplearn.chat.dto.ChatMessageData;
import co.edu.escuelaing.uplearn.chat.service.AuthorizationService;
import co.edu.escuelaing.uplearn.chat.service.ChatService;
import co.edu.escuelaing.uplearn.chat.service.ReservationClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatWebSocketGatewayTest {

    private AuthorizationService authz;
    private ChatService chatService;
    private ReservationClient reservations;
    private StringRedisTemplate redis;
    private RedisMessageListenerContainer container;
    private ChatWebSocketGateway gw;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        chatService = mock(ChatService.class);
        reservations = mock(ReservationClient.class);
        redis = mock(StringRedisTemplate.class);
        container = mock(RedisMessageListenerContainer.class);
        gw = new ChatWebSocketGateway(authz, chatService, reservations, redis, container);
    }

    @Test
    void initRedisListener_registraListener_yEntregaMensajes_OK() throws Exception {
        // Preparamos sesiones para u1 (una funciona, otra lanza IOException)
        WebSocketSession okSession = mock(WebSocketSession.class);
        WebSocketSession failingSession = mock(WebSocketSession.class);
        doThrow(new IOException("boom")).when(failingSession).sendMessage(any(TextMessage.class));

        Map<String, Set<WebSocketSession>> sessions = sessionsMap();
        sessions.clear();
        Set<WebSocketSession> set = new HashSet<>();
        set.add(okSession);
        set.add(failingSession);
        sessions.put("u1", set); // fromUserId tendrá sesiones
        // toUserId = u2 no tiene sesiones -> rama con emptySet

        ArgumentCaptor<MessageListener> listenerCap = ArgumentCaptor.forClass(MessageListener.class);
        ArgumentCaptor<Topic> topicCap = ArgumentCaptor.forClass(Topic.class);

        gw.initRedisListener();

        verify(container).addMessageListener(listenerCap.capture(), topicCap.capture());
        Topic t = topicCap.getValue();
        assertTrue(t instanceof PatternTopic);
        assertEquals("chat:*", t.getTopic());

        MessageListener listener = listenerCap.getValue();

        // Caso OK: payload válido
        ChatMessageData dto = ChatMessageData.builder()
                .id("m1")
                .fromUserId("u1")
                .toUserId("u2")
                .content("hola")
                .build();
        String jsonPayload = json.writeValueAsString(dto);

        org.springframework.data.redis.connection.Message okMsg =
                mock(org.springframework.data.redis.connection.Message.class);
        when(okMsg.getBody()).thenReturn(jsonPayload.getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> listener.onMessage(okMsg, "chat:cid".getBytes(StandardCharsets.UTF_8)));

        verify(okSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(failingSession, atLeastOnce()).sendMessage(any(TextMessage.class));

        // Caso error: payload inválido (cubre catch del listener)
        org.springframework.data.redis.connection.Message badMsg =
                mock(org.springframework.data.redis.connection.Message.class);
        when(badMsg.getBody()).thenReturn("NOT_JSON".getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> listener.onMessage(badMsg, "chat:cid".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void afterConnectionEstablished_tokenValido_entregaPendientes_OK() throws Exception {
        String token = "tok1";
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getUri()).thenReturn(new URI("ws://x/ws?token=" + token));
        when(authz.subject("Bearer " + token)).thenReturn("u1");
        when(s.getAttributes()).thenReturn(new HashMap<>());
        when(s.isOpen()).thenReturn(true);

        Message m = Message.builder().id("1").content("E").build();
        when(chatService.pendingFor("u1")).thenReturn(List.of(m));
        when(chatService.toDto(m)).thenReturn(ChatMessageData.builder()
                .id("1").fromUserId("u1").toUserId("u2").build());

        gw.afterConnectionEstablished(s);

        verify(s, atLeastOnce()).sendMessage(isA(TextMessage.class));
        verify(chatService).markDelivered(anyList());
    }

    @Test
    void afterConnectionEstablished_tokenValido_sinPendientes_OK() throws Exception {
        String token = "tok2";
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getUri()).thenReturn(new URI("ws://x/ws?token=" + token));
        when(authz.subject("Bearer " + token)).thenReturn("u2");
        when(s.getAttributes()).thenReturn(new HashMap<>());
        when(s.isOpen()).thenReturn(true);
        when(chatService.pendingFor("u2")).thenReturn(Collections.emptyList());

        gw.afterConnectionEstablished(s);

        verify(chatService, never()).markDelivered(anyList());
        verify(s, never()).sendMessage(any());
    }

    @Test
    void afterConnectionEstablished_pendienteFalla_conversion_LOG() throws Exception {
        String token = "tok3";
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getUri()).thenReturn(new URI("ws://x/ws?token=" + token));
        when(authz.subject("Bearer " + token)).thenReturn("u3");
        when(s.getAttributes()).thenReturn(new HashMap<>());
        when(s.isOpen()).thenReturn(true);

        Message m = Message.builder().id("1").content("E").build();
        when(chatService.pendingFor("u3")).thenReturn(List.of(m));
        when(chatService.toDto(m)).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> gw.afterConnectionEstablished(s));
        // La rama de catch se ejecuta; no esperamos envíos
        verify(s, never()).sendMessage(any());
        verify(chatService).markDelivered(anyList());
    }

    @Test
    void afterConnectionEstablished_sinToken_oTokenInvalido_cierra_FAIL() throws Exception {
        // Sin token
        WebSocketSession s1 = mock(WebSocketSession.class);
        when(s1.getUri()).thenReturn(new URI("ws://x/ws"));
        gw.afterConnectionEstablished(s1);
        verify(s1).close(argThat(st ->
                st.getCode() == CloseStatus.NOT_ACCEPTABLE.getCode()
                        && st.getReason().contains("Falta token")));

        // Token inválido
        WebSocketSession s2 = mock(WebSocketSession.class);
        when(s2.getUri()).thenReturn(new URI("ws://x/ws?token=bad"));
        when(authz.subject("Bearer bad")).thenThrow(new RuntimeException("bad"));
        gw.afterConnectionEstablished(s2);
        verify(s2).close(argThat(st ->
                st.getCode() == CloseStatus.NOT_ACCEPTABLE.getCode()
                        && st.getReason().contains("Token inválido")));
    }

    @Test
    void handleTextMessage_sinUserId_cierra_FAIL() throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getAttributes()).thenReturn(new HashMap<>());

        gw.handleTextMessage(s, new TextMessage("algo"));

        verify(s).close(argThat(st ->
                st.getCode() == CloseStatus.NOT_ACCEPTABLE.getCode()
                        && st.getReason().contains("No autenticado")));
    }

    @Test
    void handleTextMessage_payloadBlanco_ignora_OK() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        reset(reservations, chatService, redis);

        gw.handleTextMessage(s, new TextMessage("   "));

        verifyNoInteractions(reservations, chatService, redis);
    }

    @Test
    void handleTextMessage_pingYOtrosIgnorados_OK() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        reset(reservations, chatService, redis);

        gw.handleTextMessage(s, new TextMessage("ping"));
        gw.handleTextMessage(s, new TextMessage("{\"type\":\"PING\"}"));

        verifyNoInteractions(reservations, chatService, redis);
    }

    @Test
    void handleTextMessage_payloadInvalido_oCamposFaltantes_ignora_FAIL() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        reset(reservations, chatService, redis);

        // NO JSON -> cae en catch de parseo
        gw.handleTextMessage(s, new TextMessage("NO_JSON"));
        // JSON válido pero campos requeridos vacíos
        gw.handleTextMessage(s, new TextMessage("{\"toUserId\":\"\",\"content\":\"\"}"));

        verifyNoInteractions(reservations, chatService, redis);
    }

    @Test
    void handleTextMessage_sinAutorizacionPorReservas_enviaError_FAIL() throws Exception {
        WebSocketSession s = sessionWithUser("u1", "tokAuth");
        reset(chatService, redis); // no nos importa lo de afterConnectionEstablished

        when(reservations.canChat("Bearer tokAuth", "u2")).thenReturn(false);

        gw.handleTextMessage(s, new TextMessage("{\"toUserId\":\"u2\",\"content\":\"hola\"}"));

        verify(s).sendMessage(argThat((TextMessage tm) ->
                tm.getPayload().contains("No autorizado")));
        verify(redis, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void handleTextMessage_enviaMensaje_OK() throws Exception {
        WebSocketSession s = sessionWithUser("u1", "tokX");
        reset(reservations, chatService, redis);

        when(reservations.canChat("Bearer tokX", "u2")).thenReturn(true);
        when(chatService.chatIdOf("u1", "u2")).thenReturn("cid");
        Message saved = Message.builder().id("m1").build();
        when(chatService.saveMessage("cid", "u1", "u2", "hola")).thenReturn(saved);
        when(chatService.toDto(saved)).thenReturn(ChatMessageData.builder()
                .id("m1").fromUserId("u1").toUserId("u2").build());

        gw.handleTextMessage(s, new TextMessage("{\"toUserId\":\"u2\",\"content\":\"hola\"}"));

        verify(redis).convertAndSend(eq("chat:cid"), anyString());
    }

    @Test
    void afterConnectionClosed_remueveSesion_OK_yNoRevientaSinUser() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        assertTrue(sessionsMap().containsKey("u1"));

        gw.afterConnectionClosed(s, CloseStatus.NORMAL);
        assertFalse(sessionsMap().containsKey("u1"));

        // Sin userId -> simplemente no hace nada
        WebSocketSession s2 = mock(WebSocketSession.class);
        when(s2.getAttributes()).thenReturn(new HashMap<>());
        assertDoesNotThrow(() -> gw.afterConnectionClosed(s2, CloseStatus.NORMAL));
    }

    // ---- helpers ----

    private WebSocketSession sessionWithUser(String userId) throws Exception {
        return sessionWithUser(userId, "tok");
    }

    private WebSocketSession sessionWithUser(String userId, String token) throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getUri()).thenReturn(new URI("ws://x/ws?token=" + token));
        when(authz.subject("Bearer " + token)).thenReturn(userId);
        Map<String, Object> attrs = new HashMap<>();
        when(s.getAttributes()).thenReturn(attrs);
        when(s.isOpen()).thenReturn(true);
        when(chatService.pendingFor(userId)).thenReturn(Collections.emptyList());

        gw.afterConnectionEstablished(s);
        return s;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<WebSocketSession>> sessionsMap() {
        try {
            Field f = ChatWebSocketGateway.class.getDeclaredField("sessionsByUser");
            f.setAccessible(true);
            return (Map<String, Set<WebSocketSession>>) f.get(gw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
