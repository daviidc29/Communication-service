package co.edu.escuelaing.uplearn.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Configuración de Redis para la aplicación de chat.
 * Esta configuración se activa solo si la propiedad 'redis.enabled' está
 * establecida en true.
 */
@Configuration
@ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    @Value("${redis.host:localhost}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    /**
     * Configura la fábrica de conexiones de Redis utilizando Lettuce.
     *
     * @return una instancia de LettuceConnectionFactory configurada.
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
    }

    /**
     * Configura el StringRedisTemplate para interactuar con Redis.
     *
     * @param cf la fábrica de conexiones de Redis.
     * @return una instancia de StringRedisTemplate.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    /**
     * Configura el contenedor de escucha de mensajes de Redis.
     *
     * @param cf la fábrica de conexiones de Redis.
     * @return una instancia de RedisMessageListenerContainer.
     */
    @Bean
    public RedisMessageListenerContainer listenerContainer(LettuceConnectionFactory cf) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        return c;
    }
}
