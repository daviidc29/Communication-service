package co.edu.escuelaing.uplearn.chat.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RolesResponseTest {

    @Test
    void getters_setters_y_toString_OK() {
        RolesResponse r = new RolesResponse();
        r.setId("id");
        r.setEmail("mail");
        r.setName("name");
        r.setRoles(List.of("STUDENT", "TUTOR"));
        r.setHasRoles(true);
        r.setLastUpdated("now");

        assertEquals("id", r.getId());
        assertEquals("mail", r.getEmail());
        assertEquals("name", r.getName());
        assertEquals(List.of("STUDENT", "TUTOR"), r.getRoles());
        assertTrue(r.isHasRoles());
        assertEquals("now", r.getLastUpdated());

        String s = r.toString();
        assertTrue(s.contains("id"));
        assertTrue(s.contains("mail"));
        assertTrue(s.contains("name"));
        assertTrue(s.contains("roles"));
        assertTrue(s.contains("hasRoles"));
        assertTrue(s.contains("lastUpdated"));
    }

    @Test
    void equals_y_hashCode_cobertura_total() {
        RolesResponse r1 = new RolesResponse();
        r1.setId("id1");
        r1.setEmail("test@mail.com");
        r1.setName("User Name");
        r1.setRoles(List.of("ADMIN"));
        r1.setHasRoles(true);
        r1.setLastUpdated("2024-01-01");

        RolesResponse r2 = new RolesResponse();
        r2.setId("id1");
        r2.setEmail("test@mail.com");
        r2.setName("User Name");
        r2.setRoles(List.of("ADMIN"));
        r2.setHasRoles(true);
        r2.setLastUpdated("2024-01-01");

        assertEquals(r1, r1);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        r2.setId("id2");
        assertNotEquals(r1, r2);
        r2.setId("id1"); 

        r2.setEmail("other@mail.com");
        assertNotEquals(r1, r2);
        r2.setEmail("test@mail.com"); 

        r2.setName("Other Name");
        assertNotEquals(r1, r2);
        r2.setName("User Name"); 

        r2.setRoles(List.of("USER"));
        assertNotEquals(r1, r2);
        r2.setRoles(List.of("ADMIN")); 

        r2.setHasRoles(false);
        assertNotEquals(r1, r2);
        r2.setHasRoles(true); 

        r2.setLastUpdated("2025-01-01");
        assertNotEquals(r1, r2);
        r2.setLastUpdated("2024-01-01"); 

        assertNotEquals(null, r1);
        assertNotEquals("Soy un String", r1);
    }
}