package co.edu.escuelaing.uplearn.chat.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChatTest {

    @Test
    void builder_y_getters_OK() {
        Instant now = Instant.now();
        Set<String> participants = Set.of("u1", "u2");

        Chat chat = Chat.builder()
                .id("cid")
                .userA("u1")
                .userB("u2")
                .createdAt(now)
                .participants(participants)
                .build();

        assertEquals("cid", chat.getId());
        assertEquals("u1", chat.getUserA());
        assertEquals("u2", chat.getUserB());
        assertEquals(now, chat.getCreatedAt());
        assertEquals(participants, chat.getParticipants());
        assertTrue(chat.toString().contains("cid"));
    }

    @Test
    void equals_y_hashCode_OK_y_FAIL() {
        Instant now = Instant.now();
        Set<String> participants = Set.of("u1", "u2");

        Chat c1 = Chat.builder()
                .id("cid")
                .userA("u1")
                .userB("u2")
                .createdAt(now)
                .participants(participants)
                .build();

        Chat c2 = new Chat();
        c2.setId("cid");
        c2.setUserA("u1");
        c2.setUserB("u2");
        c2.setCreatedAt(now);
        c2.setParticipants(participants);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        
        assertEquals(c1, c1);

        Chat different = Chat.builder().id("other").userA("u1").userB("u2").build();
        assertNotEquals(c1, different);
        assertNotEquals(null, c1);
        assertNotEquals("not-a-chat", c1);
    }
    

    @Test
    void equals_verificaCadaCampo() {
        Instant now = Instant.now();
        Set<String> parts = Set.of("u1", "u2");
        
        Chat base = Chat.builder()
                .id("1")
                .userA("A")
                .userB("B")
                .createdAt(now)
                .participants(parts)
                .build();

        Chat diffId = Chat.builder().id("2").userA("A").userB("B").createdAt(now).participants(parts).build();
        assertNotEquals(base, diffId);
        assertNotEquals(base.hashCode(), diffId.hashCode());

        Chat diffUserA = Chat.builder().id("1").userA("X").userB("B").createdAt(now).participants(parts).build();
        assertNotEquals(base, diffUserA);
        
        Chat diffUserB = Chat.builder().id("1").userA("A").userB("X").createdAt(now).participants(parts).build();
        assertNotEquals(base, diffUserB);

        Chat diffDate = Chat.builder().id("1").userA("A").userB("B").createdAt(now.plusSeconds(10)).participants(parts).build();
        assertNotEquals(base, diffDate);

        Chat diffParts = Chat.builder().id("1").userA("A").userB("B").createdAt(now).participants(Set.of("X")).build();
        assertNotEquals(base, diffParts);
        
        Chat withNulls = new Chat(); 
        assertNotEquals(base, withNulls);
        assertNotEquals(withNulls, base);
    }
    
    @Test
    void canEqual_funciona() {
        Chat c = new Chat();
        assertTrue(c.canEqual(new Chat()));
        assertFalse(c.canEqual("string"));
    }


    @Test
    void builder_toString_OK() {
        String builderString = Chat.builder()
                .id("test")
                .toString();
        assertNotNull(builderString);
        assertTrue(builderString.contains("ChatBuilder"));
    }

    @Test
    void equals_conSubclase_FAIL() {
        Chat c1 = new Chat();
        Chat c2 = new Chat() {
            @Override
            public boolean canEqual(Object other) {
                return false;
            }
        };
        assertNotEquals(c1, c2);
    }
}