package co.edu.escuelaing.uplearn.chat.controller;

import co.edu.escuelaing.uplearn.chat.domain.Message;
import co.edu.escuelaing.uplearn.chat.dto.ChatContact;
import co.edu.escuelaing.uplearn.chat.dto.ChatMessageData;
import co.edu.escuelaing.uplearn.chat.dto.PublicProfile;
import co.edu.escuelaing.uplearn.chat.dto.RolesResponse;
import co.edu.escuelaing.uplearn.chat.service.AuthorizationService;
import co.edu.escuelaing.uplearn.chat.service.ChatService;
import co.edu.escuelaing.uplearn.chat.service.ReservationClient;
import co.edu.escuelaing.uplearn.chat.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatControllerTest {

    AuthorizationService authz;
    ChatService chat;
    ReservationClient reservations;
    UserServiceClient users;
    ChatController controller;

    @BeforeEach
    void setUp() {
        authz = mock(AuthorizationService.class);
        chat = mock(ChatService.class);
        reservations = mock(ReservationClient.class);
        users = mock(UserServiceClient.class);
        controller = new ChatController(authz, chat, reservations, users);
    }

    @Test
    void contacts_devuelveContactosConPerfil_OK1() {
        RolesResponse me = new RolesResponse();
        me.setId("me");
        when(authz.me("Bearer x")).thenReturn(me);
        when(reservations.counterpartIds("Bearer x", "me"))
                .thenReturn(Set.of("u1", "u2"));
        when(users.getPublicProfileById("u1"))
                .thenReturn(PublicProfile.builder().id("u1").name("User 1").email("U1@MAIL").build());
        when(users.getPublicProfileById("u2"))
                .thenReturn(PublicProfile.builder().id("u2").name("User 2").build());

        List<ChatContact> out = controller.contacts("Bearer x");
        assertEquals(2, out.size());

        var ids = out.stream().map(ChatContact::getId).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("u1", "u2"), ids);
    }

    @Test
    void contacts_siNoHayPerfilUsaFallback_OK2() {
        RolesResponse me = new RolesResponse();
        me.setId("me");
        when(authz.me("t")).thenReturn(me);
        when(reservations.counterpartIds("t", "me")).thenReturn(Set.of("z"));
        when(users.getPublicProfileById("z")).thenReturn(null);

        List<ChatContact> out = controller.contacts("t");
        assertEquals(1, out.size());
        assertEquals("Usuario", out.get(0).getName());
    }

    @Test
    void contacts_sinContrapartesVacias_FAIL1() {
        RolesResponse me = new RolesResponse();
        me.setId("me");
        when(authz.me("t")).thenReturn(me);
        when(reservations.counterpartIds("t", "me")).thenReturn(Collections.emptySet());

        List<ChatContact> out = controller.contacts("t");
        assertTrue(out.isEmpty());
    }

    @Test
    void contacts_authzFalla_PROPAGAExcepcion_FAIL2() {
        when(authz.me(anyString())).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> controller.contacts("x"));
    }

    @Test
    void history_mapeaMensajes_OK1() {
        Message m1 = Message.builder().id("1").build();
        when(chat.history("c")).thenReturn(List.of(m1));
        when(chat.toDto(m1)).thenReturn(ChatMessageData.builder().id("1").build());

        ResponseEntity<Object> rsp = controller.history("c", "B");
        assertEquals(200, rsp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        List<ChatMessageData> list = (List<ChatMessageData>) rsp.getBody();
        assertEquals(1, list.size());
    }

    @Test
    void history_listaNulaRetornaListaVacia_OK2() {
        when(chat.history("c")).thenReturn(null);
        ResponseEntity<Object> rsp = controller.history("c", "B");
        assertEquals(200, rsp.getStatusCode().value());
        assertEquals(List.of(), rsp.getBody());
    }

    @Test
    void history_siConversionFalla_omiteMensaje_FAIL1() {
        Message m1 = Message.builder().id("1").build();
        Message m2 = Message.builder().id("2").build();
        when(chat.history("c")).thenReturn(List.of(m1, m2));
        when(chat.toDto(m1)).thenThrow(new RuntimeException("bad"));
        when(chat.toDto(m2)).thenReturn(ChatMessageData.builder().id("2").build());

        ResponseEntity<Object> rsp = controller.history("c", "B");
        @SuppressWarnings("unchecked")
        List<ChatMessageData> list = (List<ChatMessageData>) rsp.getBody();
        assertEquals(1, list.size());
        assertEquals("2", list.get(0).getId());
    }

    @Test
    void history_errorGeneralDevuelve500_FAIL2() {
        when(chat.history("c")).thenThrow(new RuntimeException("boom"));
        ResponseEntity<Object> rsp = controller.history("c", "B");
        assertEquals(500, rsp.getStatusCode().value());
        assertTrue(rsp.getBody().toString().contains("Error cargando historial"));
    }

    @Test
    void chatId_okDevuelveMapa_OK1y2() {
        when(authz.subject("B")).thenReturn("me");
        when(chat.chatIdOf("me", "other")).thenReturn("cid");
        Map<String, String> out = controller.chatId("other", "B");
        assertEquals("cid", out.get("chatId"));
        assertEquals("me", out.get("meId"));
    }

    @Test
    void chatId_authzSubjectFalla_PROPAGA_FAIL1y2() {
        when(authz.subject(anyString())).thenThrow(new RuntimeException("bad"));
        assertThrows(RuntimeException.class, () -> controller.chatId("x", "B"));
        assertThrows(RuntimeException.class, () -> controller.chatId("", "B"));
    }
}
