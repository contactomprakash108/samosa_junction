package com.samosajunction.common.storage;

import com.samosajunction.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalObjectUrlSignerTest {

    private final LocalObjectUrlSigner signer = new LocalObjectUrlSigner(TestS3Properties.defaults());

    @Test
    void verifyAcceptsFreshSignature() {
        String key = "products/11111111-1111-4111-8111-111111111112/samosa.jpg";
        long exp = Instant.now().plusSeconds(60).getEpochSecond();
        var uri = signer.sign(key, Instant.ofEpochSecond(exp));
        String sig = query(uri.getQuery(), "sig");

        signer.verify(key, exp, sig);
        assertThat(uri.getPath()).isEqualTo("/api/objects");
    }

    @Test
    void verifyRejectsExpiredUrl() {
        String key = "complaints/abc/photo.jpg";
        long exp = Instant.now().minusSeconds(5).getEpochSecond();
        var uri = signer.sign(key, Instant.ofEpochSecond(exp));

        assertThatThrownBy(() -> signer.verify(key, exp, query(uri.getQuery(), "sig")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void verifyRejectsTamperedSignature() {
        String key = "complaints/abc/photo.jpg";
        long exp = Instant.now().plusSeconds(60).getEpochSecond();

        assertThatThrownBy(() -> signer.verify(key, exp, "deadbeef"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("signature");
    }

    private static String query(String query, String name) {
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair[0].equals(name)) {
                return pair[1];
            }
        }
        throw new IllegalStateException("Missing " + name);
    }
}
