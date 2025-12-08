package co.edu.escuelaing.uplearn.chat.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuración de caché utilizando Caffeine para almacenar roles y perfiles
 * públicos de usuario.
 */
@Configuration
@EnableCaching
public class CacheConfig {

        /**
         * configuración del cache manager con caches para roles y perfiles públicos de
         * usuario.
         * 
         * @return CacheManager configurado
         */
        @Bean
        public CacheManager cacheManager(
                        @Value("${roles.cache.ttl-seconds}") long rolesTtl,
                        @Value("${roles.cache.max-size}") long rolesMax,
                        @Value("${profiles.cache.ttl-seconds}") long profilesTtl,
                        @Value("${profiles.cache.max-size}") long profilesMax) {
                var rolesCaffeine = Caffeine.newBuilder()
                                .maximumSize(rolesMax)
                                .expireAfterWrite(rolesTtl, TimeUnit.SECONDS)
                                .recordStats();

                var profilesCaffeine = Caffeine.newBuilder()
                                .maximumSize(profilesMax)
                                .expireAfterWrite(profilesTtl, TimeUnit.SECONDS)
                                .recordStats();

                var manager = new SimpleCacheManager();
                manager.setCaches(Arrays.asList(
                                new CaffeineCache("userRoles", rolesCaffeine.build()),
                                new CaffeineCache("userPublicProfiles", profilesCaffeine.build())));
                return manager;
        }
}
