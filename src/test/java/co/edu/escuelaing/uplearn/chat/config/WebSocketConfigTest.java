package co.edu.escuelaing.uplearn.chat.config;

import co.edu.escuelaing.uplearn.chat.ws.ChatWebSocketGateway;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils; 
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    @Test
    void registerWebSocketHandlers_agregaHandler_OK1y2() {
        ChatWebSocketGateway gw = mock(ChatWebSocketGateway.class);
        WebSocketConfig cfg = new WebSocketConfig(gw);
        
        ReflectionTestUtils.setField(cfg, "allowed", "*");

        WebSocketHandlerRegistry reg = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration =
                mock(WebSocketHandlerRegistration.class, RETURNS_SELF);
        when(reg.addHandler(gw, "/ws/chat")).thenReturn(registration);

        cfg.registerWebSocketHandlers(reg);

        verify(reg).addHandler(gw, "/ws/chat");
        verify(registration).setAllowedOriginPatterns("*");
    }

    @Test
    void registerWebSocketHandlers_registryNull_FAIL1() {
        ChatWebSocketGateway gw = mock(ChatWebSocketGateway.class);
        WebSocketConfig cfg = new WebSocketConfig(gw);
        ReflectionTestUtils.setField(cfg, "allowed", "*"); 
        
        assertThrows(NullPointerException.class, () -> cfg.registerWebSocketHandlers(null));
    }

    @Test
    void registerWebSocketHandlers_gatewayNull_FAIL2() {
        WebSocketConfig cfg = new WebSocketConfig(null);
        ReflectionTestUtils.setField(cfg, "allowed", "*");

        WebSocketHandlerRegistry reg = mock(WebSocketHandlerRegistry.class);

        assertThrows(Exception.class, () -> cfg.registerWebSocketHandlers(reg));
    }
}