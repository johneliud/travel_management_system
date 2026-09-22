package com.travelmanagementsystem.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordHasher")
class PasswordHasherTest {

    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
    }

    @Test
    @DisplayName("hash and verify round-trip succeeds")
    void hashAndVerifyRoundTrip() {
        String raw = "my-secret-password";
        String hash = hasher.hash(raw);

        assertThat(hasher.verify(raw, hash)).isTrue();
    }

    @Test
    @DisplayName("identical passwords produce different hashes (salting)")
    void differentHashesForSamePassword() {
        String raw = "same-password";
        String hash1 = hasher.hash(raw);
        String hash2 = hasher.hash(raw);

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hasher.verify(raw, hash1)).isTrue();
        assertThat(hasher.verify(raw, hash2)).isTrue();
    }

    @Test
    @DisplayName("incorrect password is rejected")
    void incorrectPasswordRejected() {
        String hash = hasher.hash("correct-password");

        assertThat(hasher.verify("wrong-password", hash)).isFalse();
    }
}
