package co.edu.escuelaing.uplearn.chat.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendMessageRequestTest {

    @Test
    void builder_setters_getters_y_toString_OK() {
        SendMessageRequest.SendMessageRequestBuilder builder = SendMessageRequest.builder()
                .toUserId("u2")
                .content("hola");

        assertNotNull(builder.toString());

        SendMessageRequest req = builder.build();

        assertEquals("u2", req.getToUserId());
        assertEquals("hola", req.getContent());
        
        assertTrue(req.toString().contains("u2"));
        assertTrue(req.toString().contains("hola"));

        req.setToUserId("u3");
        assertEquals("u3", req.getToUserId());
        
        req.setContent("adios");
        assertEquals("adios", req.getContent());
    }

    @Test
    void equals_y_hashCode_cobertura_total() {
        SendMessageRequest r1 = new SendMessageRequest("u1", "contenido");
        SendMessageRequest r2 = new SendMessageRequest("u1", "contenido");

        assertEquals(r1, r1);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        r2.setToUserId("u2");
        assertNotEquals(r1, r2);
        assertNotEquals(r1.hashCode(), r2.hashCode()); 
        r2.setToUserId("u1"); 

        r2.setContent("otro contenido");
        assertNotEquals(r1, r2);
        r2.setContent("contenido"); 

        assertNotEquals(null, r1);

        assertNotEquals("un string", r1);
        
        assertTrue(r1.canEqual(r2));
    }
    
    @Test
    void constructor_vacio() {
        SendMessageRequest req = new SendMessageRequest();
        assertNull(req.getToUserId());
        assertNull(req.getContent());
    }
}