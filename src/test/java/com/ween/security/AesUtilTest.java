package com.ween.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesUtilTest {

    private AesUtil aesUtil;

    @BeforeEach
    void setUp() {
        aesUtil = new AesUtil();
        ReflectionTestUtils.setField(aesUtil, "aesSecretKey", "1234567890abcdef");
    }

    @Test
    void encryptAndDecryptRoundTrip() {
        String encrypted = aesUtil.encrypt("user-1:event-1");

        assertThat(encrypted).isNotEqualTo("user-1:event-1");
        assertThat(aesUtil.decrypt(encrypted)).isEqualTo("user-1:event-1");
    }

    @Test
    void decryptFailsForInvalidPayload() {
        assertThatThrownBy(() -> aesUtil.decrypt("not-valid-aes"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }
}
