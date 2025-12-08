package co.edu.escuelaing.uplearn.chat.config;

import co.edu.escuelaing.uplearn.chat.ws.ChatWebSocketGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración de WebSocket para la aplicación de chat.
 * Registra los manejadores de WebSocket y configura CORS.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketGateway chatGateway;

    @Value("${app.cors.allowed-origins:*}")
    private String allowed;

    /**
     * Registra los manejadores de WebSocket y configura los orígenes permitidos para CORS.
     * Usa patrones para permitir comodines/subdominios sin usar '*' duro.
     * 
     * @param registry el registro de manejadores de WebSocket
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatGateway, "/ws/chat")
                .setAllowedOriginPatterns(allowed.split(","));
    }
}
