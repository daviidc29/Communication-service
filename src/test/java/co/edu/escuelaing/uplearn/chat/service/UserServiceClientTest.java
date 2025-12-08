package co.edu.escuelaing.uplearn.chat.service;

import co.edu.escuelaing.uplearn.chat.dto.PublicProfile;
import co.edu.escuelaing.uplearn.chat.dto.RolesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;
import co.edu.escuelaing.uplearn.chat.TestUtils;
import static org.junit.jupiter.api.Assertions.*;

class UserServiceClientTest {

    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager("userRoles", "userPublicProfiles");
    }

    private WebClient wcReturning(Object body) {
        ExchangeFunction fx = req -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(TestUtils.toJson(body))
                        .build());
        return WebClient.builder().exchangeFunction(fx).build();
    }

    private WebClient wcError(HttpStatus status, String body) {
        ExchangeFunction fx = req -> Mono.just(
                ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(body==null?"":body)
                        .build());
        return WebClient.builder().exchangeFunction(fx).build();
    }

    private void setWc(UserServiceClient c, WebClient wc) {
        TestUtils.setField(c, "http", wc);
    }

    @Test
    void getMyRolesCached_cargaYMayusYCache_OK1y2() {
        UserServiceClient c = new UserServiceClient(cacheManager, "http://u", "/public");
        setWc(c, wcReturning(new RolesResponse(){{
            setRoles(java.util.List.of("admin","Tutor"));
        }}));

        RolesResponse r1 = c.getMyRolesCached("B");
        RolesResponse r2 = c.getMyRolesCached("B");
        assertEquals(java.util.List.of("ADMIN","TUTOR"), r1.getRoles());
        assertSame(r1, r2); // cacheado
    }

    @Test
    void getMyRolesCached_httpError_envuelveEnUserServiceException_FAIL1y2() {
        UserServiceClient c = new UserServiceClient(cacheManager, "http://u", "/public");
        setWc(c, wcError(HttpStatus.UNAUTHORIZED, "{\"error\":\"no\"}"));
        assertThrows(UserServiceClient.UserServiceException.class, () -> c.getMyRolesCached("B"));

        setWc(c, WebClient.builder().exchangeFunction(req -> Mono.error(new RuntimeException("boom"))).build());
        assertThrows(UserServiceClient.UserServiceException.class, () -> c.getMyRolesCached("B"));
    }

    @Test
    void getPublicProfileBySub_lowercaseEmailYCached_OK1y2() {
        UserServiceClient c = new UserServiceClient(cacheManager, "http://u", "/public");
        setWc(c, wcReturning(PublicProfile.builder().id("1").email("AAA@MAIL.COM").build()));
        PublicProfile p1 = c.getPublicProfileBySub("sub");
        PublicProfile p2 = c.getPublicProfileBySub("sub");
        assertEquals("aaa@mail.com", p1.getEmail());
        assertSame(p1, p2);
    }

    @Test
    void getPublicProfileBySub_httpError_devuelveNull_FAIL1y2() {
        UserServiceClient c = new UserServiceClient(cacheManager, "http://u", "/public");
        setWc(c, wcError(HttpStatus.NOT_FOUND, ""));
        assertNull(c.getPublicProfileBySub("x"));

        setWc(c, WebClient.builder().exchangeFunction(req -> Mono.error(new RuntimeException("boom"))).build());
        assertNull(c.getPublicProfileBySub("x"));
    }

    @Test
    void getPublicProfileById_lowercaseEmailYCached_OK1y2() {
        UserServiceClient c = new UserServiceClient(cacheManager, "http://u", "/public");
        setWc(c, wcReturning(PublicProfile.builder().id("1").email("A@B.COM").build()));
        PublicProfile p1 = c.getPublicProfileById("1");
        PublicProfile p2 = c.getPublicProfileById("1");
        assertEquals("a@b.com", p1.getEmail());
        assertSame(p1, p2);
    }

    @Test
    void getPublicProfileById_httpError_devuelveNull_FAIL1y2() {
        UserServiceClient c = new UserServiceClient(cacheManager, "http://u", "/public");
        setWc(c, wcError(HttpStatus.NOT_FOUND, ""));
        assertNull(c.getPublicProfileById("x"));

        setWc(c, WebClient.builder().exchangeFunction(req -> Mono.error(new RuntimeException("boom"))).build());
        assertNull(c.getPublicProfileById("x"));
    }
}
