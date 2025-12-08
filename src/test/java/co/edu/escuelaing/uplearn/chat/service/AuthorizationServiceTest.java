package co.edu.escuelaing.uplearn.chat.service;

import co.edu.escuelaing.uplearn.chat.dto.RolesResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import co.edu.escuelaing.uplearn.chat.TestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthorizationServiceTest {

    @Test
    void me_delegaAlCliente_OK1y2() {
        UserServiceClient c = mock(UserServiceClient.class);
        AuthorizationService s = new AuthorizationService(c);
        RolesResponse r = new RolesResponse();
        when(c.getMyRolesCached("t")).thenReturn(r);
        assertSame(r, s.me("t"));
        assertSame(r, s.me("t")); 
    }

    @Test
    void requireRole_cumpleIgnoraMayus_OK1() {
        UserServiceClient c = mock(UserServiceClient.class);
        AuthorizationService s = new AuthorizationService(c);
        RolesResponse r = new RolesResponse();
        r.setRoles(java.util.List.of("ADMIN","Tutor"));
        when(c.getMyRolesCached("t")).thenReturn(r);
        assertDoesNotThrow(() -> s.requireRole("t","admin"));
    }

    @Test
    void requireRole_faltaRol_FORBIDDEN_FAIL1() {
        UserServiceClient c = mock(UserServiceClient.class);
        AuthorizationService s = new AuthorizationService(c);
        RolesResponse r = new RolesResponse();
        r.setRoles(java.util.List.of("USER"));
        when(c.getMyRolesCached("t")).thenReturn(r);
        assertThrows(ResponseStatusException.class, () -> s.requireRole("t","ADMIN"));
    }

    @Test
    void requireRole_meNulo_oRolesNulos_FORBIDDEN_FAIL2() {
        UserServiceClient c = mock(UserServiceClient.class);
        AuthorizationService s = new AuthorizationService(c);
        when(c.getMyRolesCached("t")).thenReturn(null);
        assertThrows(ResponseStatusException.class, () -> s.requireRole("t","X"));

        RolesResponse r = new RolesResponse();
        when(c.getMyRolesCached("x")).thenReturn(r);
        assertThrows(ResponseStatusException.class, () -> s.requireRole("x","X"));
    }

    @Test
    void subject_aceptaBearer_yExtraeSub_OK1y2() {
        UserServiceClient c = mock(UserServiceClient.class);
        AuthorizationService s = new AuthorizationService(c);
        String jwt = TestUtils.jwtWithSub("abc");
        assertEquals("abc", s.subject("Bearer " + jwt));
        assertEquals("abc", s.subject(jwt));
    }

    @Test
    void subject_tokenInvalido_oNull_UNAUTHORIZED_FAIL1y2() {
        AuthorizationService s = new AuthorizationService(mock(UserServiceClient.class));
        assertThrows(ResponseStatusException.class, () -> s.subject(null));
        assertThrows(ResponseStatusException.class, () -> s.subject("a.b.c")); 
    }
}
