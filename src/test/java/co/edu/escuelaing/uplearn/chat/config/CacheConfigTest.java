package co.edu.escuelaing.uplearn.chat.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager; 

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    @Test
    void cacheManager_creaCachesConNombresEsperados_OK1() {
        CacheConfig cfg = new CacheConfig();
        CacheManager cm = cfg.cacheManager(60, 100, 120, 200);

        ((SimpleCacheManager) cm).afterPropertiesSet();

        assertNotNull(cm.getCache("userRoles"));
        assertNotNull(cm.getCache("userPublicProfiles"));
    }

    @Test
    void cacheManager_statsHabilitadas_OK2() {
        CacheConfig cfg = new CacheConfig();
        CacheManager cm = cfg.cacheManager(60, 100, 120, 200);

        ((SimpleCacheManager) cm).afterPropertiesSet();

        CaffeineCache roles = (CaffeineCache) cm.getCache("userRoles");
        assertNotNull(roles);
        assertNotNull(roles.getNativeCache().stats());
    }

    @Test
    void cacheManager_maxSizeNegativo_roles_FAIL1() {
        CacheConfig cfg = new CacheConfig();
        assertThrows(IllegalArgumentException.class,
                () -> cfg.cacheManager(60, -1, 120, 200));
    }

    @Test
    void cacheManager_maxSizeNegativo_profiles_FAIL2() {
        CacheConfig cfg = new CacheConfig();
        assertThrows(IllegalArgumentException.class,
                () -> cfg.cacheManager(60, 100, 120, -5));
    }
}
