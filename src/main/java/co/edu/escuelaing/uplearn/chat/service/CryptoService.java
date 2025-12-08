package co.edu.escuelaing.uplearn.chat.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Servicio de cifrado simétrico para los contenidos de los mensajes.
 * En BD el campo "content" se guarda cifrado (Base64 de iv+ciphertext).
 */
@Service
public class CryptoService {

    private final String secret;
    private SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(@Value("${chat.crypto.secret}") String secret) {
        this.secret = secret;
    }

    /** Inicializa la clave AES a partir del secreto */
    @PostConstruct
    void initKey() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] key = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar la clave AES", e);
        }
    }

    /**
     * Encripta texto plano -> Base64(iv + ciphertext)
     * 
     * @param plainText el texto plano a encriptar
     * @return el texto encriptado
     */
    public String encrypt(String plainText) {
        if (plainText == null)
            return null;
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcm);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Error encriptando mensaje", e);
        }
    }

    /**
     * Desencripta Base64(iv+ciphertext).
     * Si falla (mensajes viejos sin cifrar, etc.), devuelve el valor original.
     * 
     * @param valueFromDb el valor cifrado desde BD
     * @return el texto desencriptado
     */
    public String decrypt(String valueFromDb) {
        if (valueFromDb == null)
            return null;
        try {
            byte[] combined = Base64.getDecoder().decode(valueFromDb);
            if (combined.length < 13) {
                return valueFromDb;
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, 12);
            byte[] cipherText = Arrays.copyOfRange(combined, 12, combined.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcm);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return valueFromDb;
        }
    }
}
