package co.edu.escuelaing.uplearn.chat.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatContactTest {

    @Test
    void builder_y_getters_OK() {
        ChatContact c = ChatContact.builder()
                .id("1")
                .sub("sub")
                .name("Name")
                .email("user@mail.com")
                .avatarUrl("http://avatar")
                .build();

        assertEquals("1", c.getId());
        assertEquals("sub", c.getSub());
        assertEquals("Name", c.getName());
        assertEquals("user@mail.com", c.getEmail());
        assertEquals("http://avatar", c.getAvatarUrl());
        assertTrue(c.toString().contains("Name"));
    }

    @Test
    void constructorVacio_y_setters_OK() {
        ChatContact c = new ChatContact();
        c.setId("1");
        c.setSub("sub");
        c.setName("Name");
        c.setEmail("mail");
        c.setAvatarUrl("url");

        assertEquals("1", c.getId());
        assertEquals("sub", c.getSub());
        assertEquals("Name", c.getName());
        assertEquals("mail", c.getEmail());
        assertEquals("url", c.getAvatarUrl());
    }

    @Test
    void equals_y_hashCode_Completo() {
        ChatContact c1 = new ChatContact("1", "sub", "Name", "mail", "url");
        
        assertEquals(c1, c1);
        
        ChatContact c2 = new ChatContact("1", "sub", "Name", "mail", "url");
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        assertNotEquals(null, c1);
        assertNotEquals("string", c1);

        assertNotEquals(new ChatContact("2", "sub", "Name", "mail", "url"), c1);
        assertNotEquals(new ChatContact("1", "diff", "Name", "mail", "url"), c1);
        assertNotEquals(new ChatContact("1", "sub", "diff", "mail", "url"), c1);
        assertNotEquals(new ChatContact("1", "sub", "Name", "diff", "url"), c1);
        assertNotEquals(new ChatContact("1", "sub", "Name", "mail", "diff"), c1);
        
        ChatContact cNull = new ChatContact();
        assertNotEquals(cNull, c1);
        assertNotEquals(c1, cNull);
    }
    
    @Test
    void builderToString_OK() {
        String builderStr = ChatContact.builder().id("test").toString();
        assertNotNull(builderStr);
    }

    @Test
    void canEqual_funciona() {
        ChatContact c = new ChatContact();
        assertTrue(c.canEqual(new ChatContact()));
        assertFalse(c.canEqual(new Object()));
    }
}