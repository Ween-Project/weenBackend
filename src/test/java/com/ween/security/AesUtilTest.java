package com.ween.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesUtilTest {

    private AesUtil aesUtil;

    @BeforeEach
    void setUp() {
        aesUtil = new AesUtil();
        // 16 byte key for AES-128
        ReflectionTestUtils.setField(aesUtil, "aesSecretKey", "1234567890123456");
    }

    @Test @DisplayName("Encrypt and decrypt - round trip")
    void encryptDecrypt_roundTrip() {
        String originalText = "hello-world-123";
        String encrypted = aesUtil.encrypt(originalText);
        
        assertThat(encrypted).isNotEqualTo(originalText);
        assertThat(encrypted).isNotBlank();
        
        String decrypted = aesUtil.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(originalText);
    }

    @Test @DisplayName("Decrypt - invalid input throws RuntimeException")
    void decrypt_invalidInput() {
        assertThatThrownBy(() -> aesUtil.decrypt("not-base64-url-safe!@#"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }
}
