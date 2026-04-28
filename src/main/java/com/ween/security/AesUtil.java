package com.ween.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class AesUtil {

    @Value("${ween.aes.secret-key}")
    private String aesSecretKey;

    private static final String ALGORITHM = "AES";

    public String encrypt(String value) {
        try {
            SecretKeySpec key = new SecretKeySpec(aesSecretKey.getBytes(StandardCharsets.UTF_8), 0, 16, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedValue);
        } catch (Exception ex) {
            log.error("AES encryption failed", ex);
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            SecretKeySpec key = new SecretKeySpec(aesSecretKey.getBytes(StandardCharsets.UTF_8), 0, 16, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedValue = Base64.getUrlDecoder().decode(encryptedValue);
            byte[] decryptedValue = cipher.doFinal(decodedValue);
            return new String(decryptedValue, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("AES decryption failed", ex);
            throw new RuntimeException("Decryption failed", ex);
        }
    }
}
