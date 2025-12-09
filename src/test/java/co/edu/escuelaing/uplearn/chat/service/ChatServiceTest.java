package co.edu.escuelaing.uplearn.chat.service;

import co.edu.escuelaing.uplearn.chat.domain.Chat;
import co.edu.escuelaing.uplearn.chat.domain.Message;
import co.edu.escuelaing.uplearn.chat.dto.ChatMessageData;
import co.edu.escuelaing.uplearn.chat.repository.ChatRepository;
import co.edu.escuelaing.uplearn.chat.repository.MessageRepository;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    ChatRepository chats;
    MessageRepository messages;
    CryptoService crypto;
    MongoTemplate mongo;
    ChatService service;

    @BeforeEach
    void setUp() {
        chats = mock(ChatRepository.class);
        messages = mock(MessageRepository.class);
        crypto = mock(CryptoService.class);
        mongo = mock(MongoTemplate.class);
        service = new ChatService(chats, messages, crypto, mongo);
    }

    @Test
    void chatIdOf_conmutativo_y256hex_OK1y2() {
        String a = service.chatIdOf("A", "B");
        String b = service.chatIdOf("B", "A");
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    @Test
    void chatIdOf_conNulls_lanzaNPE_FAIL1y2() {
        assertThrows(NullPointerException.class, () -> service.chatIdOf(null, "x"));
    }

    @Test
    void ensureChat_existeDevuelveExistente_OK1() {
        Chat c = Chat.builder().id("id").userA("a").userB("b").build();
        when(chats.findByUserAAndUserB("a", "b")).thenReturn(Optional.of(c));
        assertSame(c, service.ensureChat("a", "b"));
    }

    @Test
    void ensureChat_noExisteCrea_OK2() {
        when(chats.findByUserAAndUserB("a", "b")).thenReturn(Optional.empty());
        when(chats.save(any())).thenAnswer(i -> i.getArgument(0));
        Chat out = service.ensureChat("a", "b");
        assertNotNull(out.getId());
    }

    @Test
    void saveMessage_guardaCifrado_OK1y2() {
        when(crypto.encrypt("hola")).thenReturn("ENC");
        when(messages.save(any())).thenAnswer(i -> i.getArgument(0));
        Message out = service.saveMessage("cid", "from", "to", "hola");
        assertEquals("ENC", out.getContent());
    }

    @Test
    void history_delegaRepo_OK1() {
        service.history("c");
        verify(messages).findByChatIdOrderByCreatedAtAsc("c");
    }

    @Test
    void pendingFor_delegaRepo_OK2() {
        service.pendingFor("u");
        verify(messages).findByToUserIdAndDeliveredIsFalseOrderByCreatedAtAsc("u");
    }

    @Test
    void markDelivered_marcaYGuarda_OK1y2() {
        Message m1 = new Message();
        service.markDelivered(List.of(m1));
        assertTrue(m1.isDelivered());
        verify(messages).saveAll(anyList());
    }

    @Test
    void toDto_desencripta_yConservaCampos_OK1() {
        Message m = Message.builder().content("ENC").createdAt(Instant.now()).build();
        when(crypto.decrypt("ENC")).thenReturn("plain");
        ChatMessageData dto = service.toDto(m);
        assertEquals("plain", dto.getContent());
    }
    
    @Test
    void toDto_createdAtNull_usaISOValido_OK2() {
        Message m = Message.builder().content("E").build();
        when(crypto.decrypt("E")).thenReturn("p");
        assertNotNull(service.toDto(m).getCreatedAt());
    }


    @Test
    void isParticipant_Test() {
        Chat c = Chat.builder().id("c1").participants(Set.of("A", "B")).build();
        when(chats.findById("c1")).thenReturn(Optional.of(c));
        when(chats.findById("c2")).thenReturn(Optional.empty());
        when(chats.findById("c3")).thenReturn(Optional.of(Chat.builder().id("c3").participants(null).build()));

        assertTrue(service.isParticipant("c1", "A"));
        assertFalse(service.isParticipant("c1", "C"));
        assertFalse(service.isParticipant("c2", "A"));
        assertFalse(service.isParticipant("c3", "A"));
    }

    @Test
    void historyDtoTolerant_FullDateCoverage() {
        long nowMillis = System.currentTimeMillis();
        
        Document d1 = new Document("createdAt", "2023-12-01T10:00:00Z"); 
        Document d2 = new Document("createdAt", "2023-12-01T10:00:00+05:00"); 
        Document d3 = new Document("createdAt", String.valueOf(nowMillis)); 
        Document d4 = new Document("createdAt", "invalid-date"); 
        Document d5 = new Document("createdAt", 12345); 
        
        List<Document> docs = List.of(d1, d2, d3, d4, d5);
        
        when(mongo.find(any(Query.class), eq(Document.class), eq("messages"))).thenReturn(docs);
        when(crypto.decrypt(any())).thenReturn("content");

        List<ChatMessageData> result = service.historyDtoTolerant("chat");
        
        assertEquals(5, result.size());
        
        assertNotNull(result.get(0).getCreatedAt()); 
        assertNotNull(result.get(1).getCreatedAt()); 
        assertNotNull(result.get(2).getCreatedAt()); 
        assertEquals("invalid-date", result.get(3).getCreatedAt()); 
        assertNotNull(result.get(4).getCreatedAt()); 
    }
    
    @Test
    void ensureChat_branching_sort() {
        when(chats.findByUserAAndUserB(any(), any())).thenReturn(Optional.of(new Chat()));
        
        service.ensureChat("alpha", "beta");
        verify(chats).findByUserAAndUserB("alpha", "beta");
        
        service.ensureChat("beta", "alpha");
        verify(chats, times(2)).findByUserAAndUserB("alpha", "beta");
    }
}