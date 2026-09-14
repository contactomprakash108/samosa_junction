package com.samosajunction.common.storage;

import com.samosajunction.common.exception.InvalidRequestException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

public class LocalObjectUrlSigner {

    private final S3Properties properties;

    public LocalObjectUrlSigner(S3Properties properties) {
        this.properties = properties;
    }

    public URI sign(String key, Instant expiresAt) {
        String sig = hmac(key, expiresAt.getEpochSecond());
        String base = properties.publicBaseUrl().replaceAll("/$", "");
        return URI.create(base + "/api/objects?key=" + urlEncode(key)
                + "&exp=" + expiresAt.getEpochSecond()
                + "&sig=" + sig);
    }

    public void verify(String key, long expEpochSeconds, String sig) {
        if (key.contains("..") || key.startsWith("/")) {
            throw new InvalidRequestException("Invalid object key");
        }
        if (Instant.now().getEpochSecond() > expEpochSeconds) {
            throw new InvalidRequestException("Object URL has expired");
        }
        String expected = hmac(key, expEpochSeconds);
        if (!expected.equalsIgnoreCase(sig)) {
            throw new InvalidRequestException("Object URL signature is invalid");
        }
    }

    private String hmac(String key, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.signingKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((key + ":" + exp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not sign object URL", ex);
        }
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
