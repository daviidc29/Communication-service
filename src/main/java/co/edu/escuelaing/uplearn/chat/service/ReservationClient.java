package co.edu.escuelaing.uplearn.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/** Cliente para interactuar con el servicio de reservas */
@Component
@Slf4j
public class ReservationClient {

    private final WebClient http;

    public ReservationClient(@Value("${reservations.api.base}") String base) {
        this.http = WebClient.builder().baseUrl(base).build();
    }

    /**
     * Verificar si el usuario autenticado puede chatear con otro usuario
     * 
     * @param bearer     token de autorización Bearer
     * @param withUserId ID del otro usuario
     * @return true si puede chatear, false en caso contrario
     */
    public boolean canChat(String bearer, String withUserId) {
        try {
            Map<String, Object> resp = http.get()
                    .uri(uriBuilder -> uriBuilder.path("/can-chat").queryParam("withUserId", withUserId).build())
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    }).block();
            return resp != null && Boolean.TRUE.equals(resp.get("canChat"));
        } catch (WebClientResponseException e) {
            log.warn("canChat error {} {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("canChat error {}", e.toString());
            return false;
        }
    }

    /**
     * IDs de contrapartes válidas (reservas ACEPTADO/INCUMPLIDA) para el usuario
     * autenticado
     * 
     * @param bearer token de autorización Bearer
     * @param myId   ID del usuario autenticado
     * @return conjunto de IDs de contrapartes válidas
     */
    public Set<String> counterpartIds(String bearer, String myId) {
        Set<String> out = new HashSet<>();
        try {
            List<Map<String, Object>> my = fetchReservations(bearer, "/my");
            addCounterpartsFromReservations(out, my, myId, "tutorId");
        } catch (Exception e) {
            log.debug("reservations/my fallo: {}", e.toString());
        }

        try {
            List<Map<String, Object>> forMe = fetchReservations(bearer, "/for-me");
            addCounterpartsFromReservations(out, forMe, myId, "studentId");
        } catch (Exception e) {
            log.debug("reservations/for-me fallo: {}", e.toString());
        }

        return out;
    }

    /**
     * Obtener reservas desde una URI dada
     * 
     * @param bearer token de autorización Bearer
     * @param uri    la URI del endpoint de reservas
     */
    private List<Map<String, Object>> fetchReservations(String bearer, String uri) {
        return http.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                }).collectList().block();
    }

    /**
     * Agregar IDs de contrapartes desde la lista de reservas
     * 
     * @param out          el conjunto de IDs a llenar
     * @param reservations la lista de reservas
     * @param myId         ID del usuario autenticado
     * @param idKey        la clave para obtener el ID de la contraparte en la
     *                     reserva
     */
    private void addCounterpartsFromReservations(Set<String> out, List<Map<String, Object>> reservations, String myId,
            String idKey) {
        if (reservations == null)
            return;
        for (Map<String, Object> r : reservations) {
            String status = String.valueOf(r.getOrDefault("status", ""));
            if (status.equalsIgnoreCase("ACEPTADO") || status.equalsIgnoreCase("INCUMPLIDA")) {
                String id = (String) r.get(idKey);
                if (id != null && !id.equals(myId))
                    out.add(id);
            }
        }
    }
}
