package co.edu.escuelaing.uplearn.chat.service;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    @Test
    void encryptDecrypt_roundtripTextoSimple_OK1() {
        CryptoService cs = new CryptoService("secret123");
        cs.initKey();
        String enc = cs.encrypt("hola");
        assertNotEquals("hola", enc);
        assertEquals("hola", cs.decrypt(enc));
    }

    @Test
    void encryptDecrypt_roundtripUnicode_OK2() {
        CryptoService cs = new CryptoService("secret123");
        cs.initKey();
        String enc = cs.encrypt("áé😀漢字");
        assertEquals("áé😀漢字", cs.decrypt(enc));
    }

    @Test
    void encrypt_nullDevuelveNull_FAIL1() {
        CryptoService cs = new CryptoService("secret123");
        cs.initKey();
        assertNull(cs.encrypt(null));
    }

    @Test
    void decrypt_noBase64DevuelveOriginal_FAIL2() {
        CryptoService cs = new CryptoService("secret123");
        cs.initKey();
        assertEquals("not-base64", cs.decrypt("not-base64"));
    }


    @Test
    void decrypt_nullDevuelveNull_OK() {
        CryptoService cs = new CryptoService("secret123");
        cs.initKey();
        assertNull(cs.decrypt(null));
    }

    @Test
    void decrypt_shortBytesDevuelveOriginal_OK() {
        CryptoService cs = new CryptoService("secret123");
        cs.initKey();

        String shortBase64 = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        
        String result = cs.decrypt(shortBase64);
        assertEquals(shortBase64, result);
    }

    @Test
    void encrypt_sinInicializarLanzaExcepcion() {

        CryptoService cs = new CryptoService("secret123");
        
        assertThrows(IllegalStateException.class, () -> {
            cs.encrypt("texto");
        });
    }

}