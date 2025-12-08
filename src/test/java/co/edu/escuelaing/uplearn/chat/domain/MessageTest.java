package co.edu.escuelaing.uplearn.chat.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void builder_y_getters_OK() {
        Instant now = Instant.now();

        Message msg = Message.builder()
                .id("m1")
                .chatId("c1")
                .fromUserId("u1")
                .toUserId("u2")
                .content("hola")
                .createdAt(now)
                .delivered(true)
                .read(false)
                .build();

        assertEquals("m1", msg.getId());
        assertEquals("c1", msg.getChatId());
        assertEquals("u1", msg.getFromUserId());
        assertEquals("u2", msg.getToUserId());
        assertEquals("hola", msg.getContent());
        assertEquals(now, msg.getCreatedAt());
        assertTrue(msg.isDelivered());
        assertFalse(msg.isRead());
        assertTrue(msg.toString().contains("m1"));
    }

    @Test
    void setters_OK() {
        Message msg = new Message();
        Instant now = Instant.now();

        msg.setId("m2");
        msg.setChatId("c2");
        msg.setFromUserId("u3");
        msg.setToUserId("u4");
        msg.setContent("adios");
        msg.setCreatedAt(now);
        msg.setDelivered(false);
        msg.setRead(true);

        assertEquals("m2", msg.getId());
        assertEquals("c2", msg.getChatId());
        assertEquals("u3", msg.getFromUserId());
        assertEquals("u4", msg.getToUserId());
        assertEquals("adios", msg.getContent());
        assertEquals(now, msg.getCreatedAt());
        assertFalse(msg.isDelivered());
        assertTrue(msg.isRead());
    }

    @Test
    void equals_y_hashCode_Completo() {
        Instant now = Instant.now();
        Message m1 = new Message("m1", "c1", "u1", "u2", "hola", now, true, false);

        assertEquals(m1, m1);

        Message m2 = new Message("m1", "c1", "u1", "u2", "hola", now, true, false);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        assertNotEquals(null, m1);
        assertNotEquals("string", m1);
        assertNotEquals("string", m1); 

        assertNotEquals(m1, new Message("diff", "c1", "u1", "u2", "hola", now, true, false));
        assertNotEquals(m1, new Message("m1", "diff", "u1", "u2", "hola", now, true, false));
        assertNotEquals(m1, new Message("m1", "c1", "diff", "u2", "hola", now, true, false));
        assertNotEquals(m1, new Message("m1", "c1", "u1", "diff", "hola", now, true, false));
        assertNotEquals(m1, new Message("m1", "c1", "u1", "u2", "diff", now, true, false));
        assertNotEquals(m1, new Message("m1", "c1", "u1", "u2", "hola", Instant.MIN, true, false));
        assertNotEquals(m1, new Message("m1", "c1", "u1", "u2", "hola", now, false, false)); // delivered diff
        assertNotEquals(m1, new Message("m1", "c1", "u1", "u2", "hola", now, true, true));   // read diff

        Message mNull = new Message();
        assertNotEquals(m1, mNull);
        assertNotEquals(mNull, m1);
    }
    
    @Test
    void canEqual_funciona() {
        Message m = new Message();
        assertTrue(m.canEqual(new Message()));
        assertFalse(m.canEqual(new Object()));
    }

    @Test
    void builderToString_OK() {
        String builderStr = Message.builder()
                .id("test")
                .toString();
        assertNotNull(builderStr);
    }
}