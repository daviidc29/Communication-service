package co.edu.escuelaing.uplearn.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Configuración de Redis para la aplicación de chat.
 * Configura la conexión a Redis y los beans necesarios si Redis está habilitado.
 */
@Configuration
@ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "true")
public class RedisConfig {

    @Value("${redis.host:localhost}") private String host;
    @Value("${redis.port:6379}") private int port;
    @Value("${redis.password:}") private String password;
    @Value("${redis.ssl:false}") private boolean ssl;

    /**

     * Configura la fábrica de conexiones Lettuce para Redis.
     * 
     * @return LettuceConnectionFactory configurada
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) conf.setPassword(password);
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = LettuceClientConfiguration.builder();
        if (ssl) builder.useSsl();
        return new LettuceConnectionFactory(conf, builder.build());
    }

    /**
     * Configura el StringRedisTemplate para operaciones con Redis.
     * 
     * @param cf la fábrica de conexiones Lettuce
     * @return StringRedisTemplate configurado
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    /**
     * Configura el contenedor de listeners de mensajes de Redis.
     * 
     * @param cf la fábrica de conexiones Lettuce
     * @return RedisMessageListenerContainer configurado
     */
    @Bean
    public RedisMessageListenerContainer listenerContainer(LettuceConnectionFactory cf) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        return c;
    }
}
