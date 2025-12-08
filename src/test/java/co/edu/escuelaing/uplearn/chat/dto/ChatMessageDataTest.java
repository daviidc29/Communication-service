package co.edu.escuelaing.uplearn.chat.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageDataTest {

    @Test
    void builder_y_getters_OK() {
        ChatMessageData.ChatMessageDataBuilder builder = ChatMessageData.builder()
                .id("m1")
                .chatId("c1")
                .fromUserId("u1")
                .toUserId("u2")
                .content("hola")
                .createdAt("now")
                .delivered(true)
                .read(false);

        assertNotNull(builder.toString());

        ChatMessageData d = builder.build();

        assertEquals("m1", d.getId());
        assertEquals("c1", d.getChatId());
        assertEquals("u1", d.getFromUserId());
        assertEquals("u2", d.getToUserId());
        assertEquals("hola", d.getContent());
        assertEquals("now", d.getCreatedAt());
        assertTrue(d.isDelivered());
        assertFalse(d.isRead());
        
        assertTrue(d.toString().contains("m1"));
        
        d.setId("m2");
        assertEquals("m2", d.getId());
        d.setChatId("c2");
        assertEquals("c2", d.getChatId());
        d.setFromUserId("u3");
        assertEquals("u3", d.getFromUserId());
        d.setToUserId("u4");
        assertEquals("u4", d.getToUserId());
        d.setContent("adios");
        assertEquals("adios", d.getContent());
        d.setCreatedAt("later");
        assertEquals("later", d.getCreatedAt());
        d.setDelivered(false);
        assertFalse(d.isDelivered());
        d.setRead(true);
        assertTrue(d.isRead());
    }

    @Test
    void equals_y_hashCode_Completo() {
        ChatMessageData d1 = new ChatMessageData("m1", "c1", "u1", "u2", "hola", "now", true, false);
        ChatMessageData d2 = new ChatMessageData("m1", "c1", "u1", "u2", "hola", "now", true, false);
        ChatMessageData d3 = new ChatMessageData("m2", "c1", "u1", "u2", "hola", "now", true, false);

        assertEquals(d1, d1); 
        
        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());

        assertNotEquals(null, d1);

        assertNotEquals("un string", d1);

        assertNotEquals(d1, d3);
        assertNotEquals(d1.hashCode(), d3.hashCode());
        
        assertTrue(d1.canEqual(d2));
    }

    @Test
    void constructor_NoArgs_OK() {
        ChatMessageData d = new ChatMessageData();
        assertNull(d.getId());
    }
}