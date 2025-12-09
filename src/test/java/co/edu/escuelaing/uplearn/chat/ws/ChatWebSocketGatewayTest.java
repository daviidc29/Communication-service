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
    void initRedisListener_conContainerNull_noFalla() {
        ChatWebSocketGateway localGw = new ChatWebSocketGateway(authz, chatService, reservations, redis, null);
        assertDoesNotThrow(localGw::initRedisListener);
    }

    @Test
    void initRedisListener_registraListener_yEntregaMensajes_OK() throws Exception {
        WebSocketSession okSession = mock(WebSocketSession.class);
        WebSocketSession failingSession = mock(WebSocketSession.class);
        doThrow(new IOException("boom")).when(failingSession).sendMessage(any(TextMessage.class));

        Map<String, Set<WebSocketSession>> sessions = sessionsMap();
        sessions.clear();
        Set<WebSocketSession> set = new HashSet<>();
        set.add(okSession);
        set.add(failingSession);
        sessions.put("u1", set);

        ArgumentCaptor<MessageListener> listenerCap = ArgumentCaptor.forClass(MessageListener.class);
        ArgumentCaptor<Topic> topicCap = ArgumentCaptor.forClass(Topic.class);

        gw.initRedisListener();

        verify(container).addMessageListener(listenerCap.capture(), topicCap.capture());
        Topic t = topicCap.getValue();
        assertTrue(t instanceof PatternTopic);
        assertEquals("chat:*", t.getTopic());

        MessageListener listener = listenerCap.getValue();

        ChatMessageData dto = ChatMessageData.builder()
                .id("m1")
                .fromUserId("u1")
                .toUserId("u2")
                .content("hola")
                .build();
        String jsonPayload = json.writeValueAsString(dto);

        org.springframework.data.redis.connection.Message okMsg = mock(
                org.springframework.data.redis.connection.Message.class);
        when(okMsg.getBody()).thenReturn(jsonPayload.getBytes(StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> listener.onMessage(okMsg, "chat:cid".getBytes(StandardCharsets.UTF_8)));

        verify(okSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        verify(failingSession, atLeastOnce()).sendMessage(any(TextMessage.class));

        org.springframework.data.redis.connection.Message badMsg = mock(
                org.springframework.data.redis.connection.Message.class);
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
        verify(s, never()).sendMessage(any());
        verify(chatService).markDelivered(anyList());
    }

    @Test
    void afterConnectionEstablished_sinToken_oTokenInvalido_cierra_FAIL() throws Exception {
        WebSocketSession s1 = mock(WebSocketSession.class);
        when(s1.getUri()).thenReturn(new URI("ws://x/ws"));
        gw.afterConnectionEstablished(s1);
        verify(s1).close(argThat(st -> st.getCode() == CloseStatus.NOT_ACCEPTABLE.getCode()
                && st.getReason().contains("Falta token")));

        WebSocketSession s2 = mock(WebSocketSession.class);
        when(s2.getUri()).thenReturn(new URI("ws://x/ws?token=bad"));
        when(authz.subject("Bearer bad")).thenThrow(new RuntimeException("bad"));
        gw.afterConnectionEstablished(s2);
        verify(s2).close(argThat(st -> st.getCode() == CloseStatus.NOT_ACCEPTABLE.getCode()
                && st.getReason().contains("Token inválido")));
    }

    @Test
    void handleTextMessage_Exito_DestinatarioOnline_RedisOK() throws Exception {
        WebSocketSession sSender = sessionWithUser("u1", "tokA");
        WebSocketSession sRecipient = sessionWithUser("u2", "tokB");

        when(reservations.canChat("Bearer tokA", "u2")).thenReturn(true);

        Message savedMsg = Message.builder()
                .id("msg1")
                .chatId("chat123")
                .fromUserId("u1")
                .toUserId("u2")
                .content("ENC")
                .build();
        when(chatService.chatIdOf("u1", "u2")).thenReturn("chat123");
        when(chatService.saveMessage("chat123", "u1", "u2", "Hello")).thenReturn(savedMsg);

        ChatMessageData dto = ChatMessageData.builder().id("msg1").content("Hello").build();
        when(chatService.toDto(savedMsg)).thenReturn(dto);

        gw.handleTextMessage(sSender, new TextMessage("{\"toUserId\":\"u2\",\"content\":\"Hello\"}"));

        verify(chatService).ensureChat("u1", "u2");
        verify(chatService).saveMessage("chat123", "u1", "u2", "Hello");

        verify(sSender).sendMessage(any(TextMessage.class));
        verify(sRecipient).sendMessage(any(TextMessage.class));

        verify(chatService).markDelivered(anyList());
        assertTrue(savedMsg.isDelivered());

        verify(redis).convertAndSend(eq("chat:chat123"), anyString());
    }

    @Test
    void handleTextMessage_Exito_DestinatarioOffline_RedisOK() throws Exception {
        WebSocketSession sSender = sessionWithUser("u1", "tokA");

        when(reservations.canChat("Bearer tokA", "uOffline")).thenReturn(true);

        Message savedMsg = Message.builder()
                .id("msg2")
                .chatId("chat999")
                .fromUserId("u1")
                .toUserId("uOffline")
                .content("ENC")
                .delivered(false)
                .build();

        when(chatService.chatIdOf("u1", "uOffline")).thenReturn("chat999");
        when(chatService.saveMessage("chat999", "u1", "uOffline", "Hi")).thenReturn(savedMsg);

        when(chatService.toDto(savedMsg)).thenReturn(ChatMessageData.builder().content("Hi").build());

        gw.handleTextMessage(sSender, new TextMessage("{\"toUserId\":\"uOffline\",\"content\":\"Hi\"}"));

        verify(sSender).sendMessage(any(TextMessage.class));

        assertFalse(savedMsg.isDelivered());
        verify(chatService, never()).markDelivered(anyList());

        verify(redis).convertAndSend(eq("chat:chat999"), anyString());
    }

    @Test
    void handleTextMessage_Exito_MarkDeliveredFalla_NoRompeFlujo() throws Exception {
        WebSocketSession sSender = sessionWithUser("u1", "tokA");
        sessionWithUser("u2", "tokB"); 

        when(reservations.canChat("Bearer tokA", "u2")).thenReturn(true);

        Message savedMsg = Message.builder().id("m").chatId("c").build();
        when(chatService.saveMessage(any(), any(), any(), any())).thenReturn(savedMsg);
        when(chatService.toDto(savedMsg)).thenReturn(ChatMessageData.builder().build());

        doThrow(new RuntimeException("DB error")).when(chatService).markDelivered(anyList());

        assertDoesNotThrow(
                () -> gw.handleTextMessage(sSender, new TextMessage("{\"toUserId\":\"u2\",\"content\":\"H\"}")));

        verify(redis).convertAndSend(eq("chat:c"), anyString());
    }

    @Test
    void handleTextMessage_RedisNull_NoPublica() throws Exception {
        gw = new ChatWebSocketGateway(authz, chatService, reservations, null, container);
        WebSocketSession s = sessionWithUser("u1", "tok");

        when(reservations.canChat(any(), any())).thenReturn(true);
        Message m = Message.builder().chatId("c").build();
        when(chatService.saveMessage(any(), any(), any(), any())).thenReturn(m);
        when(chatService.toDto(m)).thenReturn(ChatMessageData.builder().build());

        gw.handleTextMessage(s, new TextMessage("{\"toUserId\":\"u2\",\"content\":\"H\"}"));

        verifyNoInteractions(redis);
    }

    @Test
    void handleTextMessage_sinUserId_cierra_FAIL() throws Exception {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getAttributes()).thenReturn(new HashMap<>());

        gw.handleTextMessage(s, new TextMessage("algo"));

        verify(s).close(argThat(st -> st.getCode() == CloseStatus.NOT_ACCEPTABLE.getCode()
                && st.getReason().contains("No autenticado")));
    }

    @Test
    void handleTextMessage_payloadBlanco_ignora_OK() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        clearInvocations(reservations, chatService, redis);

        gw.handleTextMessage(s, new TextMessage("   "));

        verifyNoInteractions(reservations, chatService, redis);
    }

    @Test
    void handleTextMessage_pingYOtrosIgnorados_OK() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        clearInvocations(reservations, chatService, redis);

        gw.handleTextMessage(s, new TextMessage("ping"));
        gw.handleTextMessage(s, new TextMessage("{\"type\":\"PING\"}"));

        verifyNoInteractions(reservations, chatService, redis);
    }

    @Test
    void handleTextMessage_payloadInvalido_oCamposFaltantes_ignora_FAIL() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        clearInvocations(reservations, chatService, redis);

        gw.handleTextMessage(s, new TextMessage("NO_JSON"));
        gw.handleTextMessage(s, new TextMessage("{\"toUserId\":\"\",\"content\":\"\"}"));

        verifyNoInteractions(reservations, chatService, redis);
    }

    @Test
    void handleTextMessage_sinAutorizacionPorReservas_enviaError_FAIL() throws Exception {
        WebSocketSession s = sessionWithUser("u1", "tokAuth");
        
        when(reservations.canChat("Bearer tokAuth", "u2")).thenReturn(false);

        gw.handleTextMessage(s, new TextMessage("{\"toUserId\":\"u2\",\"content\":\"hola\"}"));

        verify(s).sendMessage(argThat((TextMessage tm) -> tm.getPayload().contains("No autorizado")));
        verify(redis, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void afterConnectionClosed_remueveSesion_OK_yNoRevientaSinUser() throws Exception {
        WebSocketSession s = sessionWithUser("u1");
        assertTrue(sessionsMap().containsKey("u1"));

        gw.afterConnectionClosed(s, CloseStatus.NORMAL);
        assertFalse(sessionsMap().containsKey("u1"));

        WebSocketSession s2 = mock(WebSocketSession.class);
        when(s2.getAttributes()).thenReturn(new HashMap<>());
        assertDoesNotThrow(() -> gw.afterConnectionClosed(s2, CloseStatus.NORMAL));
    }


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