package com.samosajunction.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// AI-ASSISTED: Cursor
// PROMPT: Shared secure token hashing and opaque token generation
// ACCEPTED-BY: omprakash
public final class TokenHasher {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private TokenHasher() {
    }

    public static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for opaque token hashing", ex);
        }
    }
}
