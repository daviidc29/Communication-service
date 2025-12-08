package co.edu.escuelaing.uplearn.chat.service;

import co.edu.escuelaing.uplearn.chat.domain.Chat;
import co.edu.escuelaing.uplearn.chat.domain.Message;
import co.edu.escuelaing.uplearn.chat.dto.ChatMessageData;
import co.edu.escuelaing.uplearn.chat.repository.ChatRepository;
import co.edu.escuelaing.uplearn.chat.repository.MessageRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
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
        assertTrue(a.matches("[0-9a-f]{64}"));
    }

    @Test
    void chatIdOf_conNulls_lanzaNPE_FAIL1y2() {
        assertThrows(NullPointerException.class, () -> service.chatIdOf(null, "x"));
        assertThrows(NullPointerException.class, () -> service.chatIdOf("x", null));
    }

    @Test
    void ensureChat_existeDevuelveExistente_OK1() {
        Chat c = Chat.builder().id("id").userA("a").userB("b").build();
        when(chats.findByUserAAndUserB("a", "b")).thenReturn(Optional.of(c));
        Chat out = service.ensureChat("a", "b");
        assertSame(c, out);
    }

    @Test
    void ensureChat_noExisteCrea_OK2() {
        when(chats.findByUserAAndUserB("a", "b")).thenReturn(Optional.empty());
        when(chats.save(any())).thenAnswer(i -> i.getArgument(0));
        Chat out = service.ensureChat("a", "b");
        assertNotNull(out.getId());
        assertEquals(Set.of("a", "b"), out.getParticipants());
    }

    @Test
    void ensureChat_repoFindFalla_PROPAGA_FAIL1() {
        when(chats.findByUserAAndUserB(anyString(), anyString()))
                .thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> service.ensureChat("a", "b"));
    }

    @Test
    void ensureChat_repoSaveFalla_PROPAGA_FAIL2() {
        when(chats.findByUserAAndUserB(anyString(), anyString())).thenReturn(Optional.empty());
        when(chats.save(any())).thenThrow(new RuntimeException("boom"));
        assertThrows(RuntimeException.class, () -> service.ensureChat("a", "b"));
    }

    @Test
    void saveMessage_guardaCifrado_OK1y2() {
        when(crypto.encrypt("hola")).thenReturn("ENC");
        when(messages.save(any())).thenAnswer(i -> {
            Message m = i.getArgument(0);
            m.setId("1");
            return m;
        });
        Message out = service.saveMessage("cid", "from", "to", "hola");
        assertEquals("cid", out.getChatId());
        assertEquals("ENC", out.getContent());
    }

    @Test
    void saveMessage_cryptoFalla_PROPAGA_FAIL1() {
        when(crypto.encrypt(anyString())).thenThrow(new IllegalStateException("bad"));

        assertThrows(IllegalStateException.class,
                () -> service.saveMessage("c", "f", "t", "x"));

        verify(messages, never()).save(any());
    }

    @Test
    void saveMessage_repoSaveFalla_PROPAGA_FAIL2() {
        when(crypto.encrypt("ok")).thenReturn("enc");
        when(messages.save(any())).thenThrow(new RuntimeException("store-fail"));

        assertThrows(RuntimeException.class,
                () -> service.saveMessage("c", "f", "t", "ok"));
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
    void history_repoFalla_PROPAGA_FAIL1() {
        when(messages.findByChatIdOrderByCreatedAtAsc("c")).thenThrow(new RuntimeException("x"));
        assertThrows(RuntimeException.class, () -> service.history("c"));
    }

    @Test
    void pendingFor_repoFalla_PROPAGA_FAIL2() {
        when(messages.findByToUserIdAndDeliveredIsFalseOrderByCreatedAtAsc("u")).thenThrow(new RuntimeException("x"));
        assertThrows(RuntimeException.class, () -> service.pendingFor("u"));
    }

    @Test
    void markDelivered_marcaYGuarda_OK1y2() {
        Message m1 = new Message();
        Message m2 = new Message();
        service.markDelivered(List.of(m1, m2));
        assertTrue(m1.isDelivered());
        assertTrue(m2.isDelivered());
        verify(messages).saveAll(anyList());
    }

    @Test
    void markDelivered_repoFalla_PROPAGA_FAIL1() {
        doThrow(new RuntimeException("bad")).when(messages).saveAll(anyList());
        assertThrows(RuntimeException.class, () -> service.markDelivered(List.of(new Message())));
    }

    @Test
    void toDto_desencripta_yConservaCampos_OK1() {
        Message m = Message.builder()
                .id("1").chatId("c").fromUserId("f").toUserId("t")
                .content("ENC").createdAt(Instant.now()).delivered(false).read(false)
                .build();
        when(crypto.decrypt("ENC")).thenReturn("plain");
        ChatMessageData dto = service.toDto(m);
        assertEquals("plain", dto.getContent());
        assertEquals("c", dto.getChatId());
    }

    @Test
    void toDto_createdAtNull_usaISOValido_OK2() {
        Message m = Message.builder()
                .content("E").build();
        when(crypto.decrypt("E")).thenReturn("p");
        ChatMessageData dto = service.toDto(m);
        assertNotNull(dto.getCreatedAt());
        assertTrue(dto.getCreatedAt().length() >= 10);
    }

    @Test
    void toDto_cryptoFalla_PROPAGA_FAIL1() {
        Message m = Message.builder().content("X").build();
        when(crypto.decrypt("X")).thenThrow(new IllegalStateException("boom"));
        assertThrows(IllegalStateException.class, () -> service.toDto(m));
    }

    @Test
    void toDto_mensajeNull_FAIL2() {
        assertThrows(NullPointerException.class, () -> service.toDto(null));
    }

    @Test
    void historyDtoTolerant_leeVariosTiposFechas_OK1y2() {
        Document d1 = new Document("_id", "1")
                .append("chatId", "c").append("fromUserId", "f").append("toUserId", "t")
                .append("content", "E1").append("createdAt", Date.from(Instant.parse("2020-01-01T00:00:00Z")))
                .append("delivered", true).append("read", 0);
        Document d2 = new Document("_id", "2")
                .append("chatId", "c").append("fromUserId", "f").append("toUserId", "t")
                .append("content", "E2").append("createdAt", new ObjectId())
                .append("delivered", "true").append("read", "1");

        when(mongo.find(any(Query.class), eq(Document.class), eq("messages"))).thenReturn(List.of(d1, d2));
        when(crypto.decrypt("E1")).thenReturn("p1");
        when(crypto.decrypt("E2")).thenReturn("p2");

        List<ChatMessageData> out = service.historyDtoTolerant("c");
        assertEquals(2, out.size());
        assertEquals("p1", out.get(0).getContent());
        assertEquals("p2", out.get(1).getContent());
    }

    @Test
    void historyDtoTolerant_mongoFalla_oCryptoFalla_PROPAGA_FAIL1y2() {
        when(mongo.find(any(Query.class), eq(Document.class), eq("messages")))
                .thenThrow(new RuntimeException("db"));
        assertThrows(RuntimeException.class, () -> service.historyDtoTolerant("c"));

        Document d = new Document("_id", "1").append("chatId", "c").append("content", "E");
        when(mongo.find(any(Query.class), eq(Document.class), eq("messages"))).thenReturn(List.of(d));
        when(crypto.decrypt("E")).thenThrow(new IllegalStateException("enc"));
        assertThrows(IllegalStateException.class, () -> service.historyDtoTolerant("c"));
    }

    @Test
    void historyDtoTolerant_toBool_coberturaCompleta() {

        Document d1 = new Document("delivered", true).append("content", "E");
        
        Document d2 = new Document("delivered", 1).append("content", "E");
        
        Document d3 = new Document("delivered", 0).append("content", "E");
        
        Document d4 = new Document("delivered", "TRUE").append("content", "E");
        
        Document d5 = new Document("delivered", "1").append("content", "E");
        
        Document d6 = new Document("delivered", "foo").append("content", "E");
        
        Document d7 = new Document("delivered", new Object()).append("content", "E");

        List<Document> docs = List.of(d1, d2, d3, d4, d5, d6, d7);
        
        when(mongo.find(any(Query.class), eq(Document.class), eq("messages"))).thenReturn(docs);
        when(crypto.decrypt("E")).thenReturn("p");

        List<ChatMessageData> result = service.historyDtoTolerant("c");
        
        assertTrue(result.get(0).isDelivered());
        assertTrue(result.get(1).isDelivered());
        assertFalse(result.get(2).isDelivered());
        assertTrue(result.get(3).isDelivered());
        assertTrue(result.get(4).isDelivered());
        assertFalse(result.get(5).isDelivered());
        assertFalse(result.get(6).isDelivered());
    }
    
    @Test
    void asStr_manejaNull() {

        Document d1 = new Document().append("content", "E"); 
        when(mongo.find(any(Query.class), eq(Document.class), eq("messages"))).thenReturn(List.of(d1));
        when(crypto.decrypt("E")).thenReturn("p");
        
        List<ChatMessageData> result = service.historyDtoTolerant("c");
        assertEquals("", result.get(0).getChatId()); 
    }

    @Test
    void ensureChat_cubreComparacionDeStrings() {
        when(chats.findByUserAAndUserB(anyString(), anyString())).thenReturn(Optional.empty());
        when(chats.save(any())).thenAnswer(i -> i.getArgument(0));

        Chat c1 = service.ensureChat("alice", "bob");
        assertEquals("alice", c1.getUserA());
        assertEquals("bob", c1.getUserB());

        Chat c2 = service.ensureChat("bob", "alice");
        assertEquals("alice", c2.getUserA());
        assertEquals("bob", c2.getUserB());
    }
}
