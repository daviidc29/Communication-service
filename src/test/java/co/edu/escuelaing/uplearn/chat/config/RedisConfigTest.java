package co.edu.escuelaing.uplearn.chat.config;

import co.edu.escuelaing.uplearn.chat.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    @Test
    void redisConnectionFactory_creaFactory_OK1() {
        RedisConfig cfg = new RedisConfig();
        TestUtils.setField(cfg, "host", "localhost");
        TestUtils.setField(cfg, "port", 6379);
        LettuceConnectionFactory f = cfg.redisConnectionFactory();
        assertNotNull(f);
    }

    @Test
    void stringRedisTemplate_inyectaFactory_OK2() {
        RedisConfig cfg = new RedisConfig();
        TestUtils.setField(cfg, "host", "localhost");
        TestUtils.setField(cfg, "port", 6379);
        LettuceConnectionFactory f = cfg.redisConnectionFactory();
        StringRedisTemplate t = cfg.stringRedisTemplate(f);
        assertNotNull(t);
        assertSame(f, t.getConnectionFactory());
    }

    @Test
    void stringRedisTemplate_factoryNull_FAIL1() {
        RedisConfig cfg = new RedisConfig();
        assertThrows(IllegalStateException.class, () -> cfg.stringRedisTemplate(null));
    }

    @Test
    void listenerContainer_factoryNull_FAIL2() {
        RedisConfig cfg = new RedisConfig();
        assertThrows(IllegalArgumentException.class, () -> cfg.listenerContainer(null));
    }
}
