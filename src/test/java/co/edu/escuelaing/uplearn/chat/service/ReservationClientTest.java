package co.edu.escuelaing.uplearn.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.*;

import java.lang.reflect.Field;
import java.util.*;

import co.edu.escuelaing.uplearn.chat.TestUtils;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class ReservationClientTest {

    ReservationClient client;

    @BeforeEach
    void setUp() {
        client = new ReservationClient("http://base");
    }

    private void setWebClient(WebClient wc) {
        try {
            Field f = ReservationClient.class.getDeclaredField("http");
            f.setAccessible(true);
            f.set(client, wc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WebClient webClientReturning(Object body) {
        ExchangeFunction fx = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(TestUtils.toJson(body))
                        .build());
        return WebClient.builder().exchangeFunction(fx).build();
    }

    private WebClient webClientError(HttpStatus status) {
        ExchangeFunction fx = request -> Mono.just(
                ClientResponse.create(status).build());
        return WebClient.builder().exchangeFunction(fx).build();
    }

    @Test
    void canChat_trueYFalse_OK1y2() {
        setWebClient(webClientReturning(Map.of("canChat", true)));
        assertTrue(client.canChat("B", "x"));

        setWebClient(webClientReturning(Map.of("canChat", false)));
        assertFalse(client.canChat("B", "x"));
    }

    @Test
    void canChat_404oExcepcion_devuelveFalse_FAIL1y2() {
        setWebClient(webClientError(HttpStatus.NOT_FOUND));
        assertFalse(client.canChat("B", "x"));

        setWebClient(WebClient.builder().exchangeFunction(req -> Mono.error(new RuntimeException("boom"))).build());
        assertFalse(client.canChat("B", "x"));
    }

    @Test
    void counterpartIds_filtraEstadosYExcluyeMiId_OK1y2() {
        List<Map<String, Object>> my = List.of(
                Map.of("status", "ACEPTADO", "tutorId", "t1"),
                Map.of("status", "CANCELADO", "tutorId", "t2"),
                Map.of("status", "INCUMPLIDA", "tutorId", "me"));
        List<Map<String, Object>> forMe = List.of(
                Map.of("status", "INCUMPLIDA", "studentId", "s1"),
                Map.of("status", "ACEPTADO", "studentId", "s2"));

        WebClient wc = WebClient.builder().exchangeFunction(req -> {
            if (req.url().toString().endsWith("/my")) {
                return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(TestUtils.toJsonArray(my))
                                .build());
            } else { 
                return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(TestUtils.toJsonArray(forMe))
                                .build());
            }
        }).build();
        setWebClient(wc);

        Set<String> out = client.counterpartIds("B", "me");
        assertEquals(Set.of("t1", "s1", "s2"), out);
    }

    @Test
    void counterpartIds_unEndpointFalla_sigueConElOtro_FAIL1y2() {
        WebClient wc = WebClient.builder().exchangeFunction(req -> {
            if (req.url().toString().endsWith("/my")) {
                return Mono.error(new RuntimeException("boom"));
            } else { 
                List<Map<String, Object>> forMe = List.of(
                        Map.of("status", "ACEPTADO", "studentId", "a"));
                return Mono.just(
                        ClientResponse.create(HttpStatus.OK)
                                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .body(TestUtils.toJsonArray(forMe))
                                .build());
            }
        }).build();
        setWebClient(wc);

        Set<String> out = client.counterpartIds("B", "me");
        assertEquals(Set.of("a"), out);
    }
}
