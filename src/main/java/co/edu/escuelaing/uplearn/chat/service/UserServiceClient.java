package co.edu.escuelaing.uplearn.chat.service;

import co.edu.escuelaing.uplearn.chat.dto.PublicProfile;
import co.edu.escuelaing.uplearn.chat.dto.RolesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Locale;

/** Cliente para interactuar con el servicio de usuarios */
@Component
@Slf4j
public class UserServiceClient {

    private final WebClient http;
    private final Cache rolesCache;
    private final Cache profilesCache;
    private final String publicPath;

    @Autowired
    public UserServiceClient(CacheManager cacheManager,
            @Value("${users.api.base}") String usersBase,
            @Value("${users.public.path}") String publicPath) {
        this.http = WebClient.builder().baseUrl(usersBase).build();
        this.rolesCache = cacheManager.getCache("userRoles");
        this.profilesCache = cacheManager.getCache("userPublicProfiles");
        this.publicPath = publicPath;
    }

    /**
     * Obtener roles del usuario autenticado (con caché)
     * 
     * @param bearer el token Bearer del usuario
     * @return los roles del usuario autenticado
     */
    public RolesResponse getMyRolesCached(String bearer) {
        String key = "me:" + bearer.hashCode();
        RolesResponse c = rolesCache.get(key, RolesResponse.class);
        if (c != null)
            return c;
        try {
            RolesResponse resp = http.get()
                    .uri("/my-roles")
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(RolesResponse.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("Users /my-roles error: {} {}", ex.getStatusCode().value(),
                                ex.getResponseBodyAsString());
                        return Mono.error(ex);
                    })
                    .block();
            if (resp != null) {
                if (resp.getRoles() != null) {
                    resp.setRoles(
                            resp.getRoles().stream().map(r -> r == null ? null : r.toUpperCase(Locale.ROOT)).toList());
                }
                rolesCache.put(key, resp);
            }
            return resp;
        } catch (Exception e) {
            throw new UserServiceException("Error llamando Users /my-roles", e);
        }
    }

    /**
     * Obtener perfil público por sub (preferido)
     * 
     * @param sub el subject (sub) del usuario
     * @return el perfil público del usuario
     */
    public PublicProfile getPublicProfileBySub(String sub) {
        String key = "sub:" + sub;
        PublicProfile cached = profilesCache.get(key, PublicProfile.class);
        if (cached != null)
            return cached;
        try {
            PublicProfile p = http.get()
                    .uri(publicPath + "/{sub}", sub)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PublicProfile.class)
                    .block();
            if (p != null) {
                if (p.getEmail() != null)
                    p.setEmail(p.getEmail().trim().toLowerCase(Locale.ROOT));
                profilesCache.put(key, p);
            }
            return p;
        } catch (Exception e) {
            log.warn("Fallo obteniendo perfil público por sub={}, intentando por id: {}", sub, e.toString());
            return null;
        }
    }

    /**
     * Obtener perfil público por id (fallback)
     * 
     * @param id el ID del usuario
     * @return el perfil público del usuario
     */
    public PublicProfile getPublicProfileById(String id) {
        String key = "id:" + id;
        PublicProfile cached = profilesCache.get(key, PublicProfile.class);
        if (cached != null)
            return cached;
        try {
            PublicProfile p = http.get()
                    .uri(publicPath + "/by-id/{id}", id)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(PublicProfile.class)
                    .block();
            if (p != null) {
                if (p.getEmail() != null)
                    p.setEmail(p.getEmail().trim().toLowerCase(Locale.ROOT));
                profilesCache.put(key, p);
            }
            return p;
        } catch (Exception e) {
            log.warn("No se encontró perfil público por id={}", id);
            return null;
        }
    }

    public static class UserServiceException extends RuntimeException {
        public UserServiceException(String message) {
            super(message);
        }

        public UserServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
