package co.edu.escuelaing.uplearn.chat.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublicProfileTest {

    @Test
    void builder_setters_getters_toString_OK() {
        PublicProfile.PublicProfileBuilder builder = PublicProfile.builder()
                .id("id")
                .sub("sub")
                .name("Name")
                .email("mail")
                .avatarUrl("url");

        assertNotNull(builder.toString());

        PublicProfile p = builder.build();

        assertEquals("id", p.getId());
        assertEquals("sub", p.getSub());
        assertEquals("Name", p.getName());
        assertEquals("mail", p.getEmail());
        assertEquals("url", p.getAvatarUrl());

        p.setId("id2");
        p.setSub("sub2");
        p.setName("Name2");
        p.setEmail("mail2");
        p.setAvatarUrl("url2");

        assertEquals("id2", p.getId());
        assertEquals("sub2", p.getSub());
        assertEquals("Name2", p.getName());
        assertEquals("mail2", p.getEmail());
        assertEquals("url2", p.getAvatarUrl());

        String s = p.toString();
        assertTrue(s.contains("PublicProfile"));
        assertTrue(s.contains("id2"));
    }

    @Test
    void equals_y_hashCode_Completo() {
        PublicProfile p1 = new PublicProfile("id", "sub", "Name", "e", "url");
        PublicProfile p2 = new PublicProfile("id", "sub", "Name", "e", "url");
        PublicProfile p3 = new PublicProfile("OTHER", "sub", "Name", "e", "url");

        assertEquals(p1, p1);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        
        assertTrue(p1.canEqual(p2));

        assertNotEquals(null, p1);

        assertNotEquals("No soy un PublicProfile", p1);

        assertNotEquals(p1, p3);
        assertNotEquals(p1.hashCode(), p3.hashCode());
    }

    @Test
    void constructors_OK() {
        PublicProfile empty = new PublicProfile();
        assertNull(empty.getId());

        PublicProfile full = new PublicProfile("1", "2", "3", "4", "5");
        assertEquals("1", full.getId());
    }
}