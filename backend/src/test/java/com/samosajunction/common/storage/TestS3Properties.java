package com.samosajunction.common.storage;

import java.time.Duration;

public final class TestS3Properties {

    private TestS3Properties() {
    }

    public static S3Properties defaults() {
        return of(5_000_000, 5, "local-dev-object-signing-key-32b!");
    }

    public static byte[] jpeg(int size) {
        byte[] bytes = new byte[Math.max(size, 12)];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        return bytes;
    }

    public static S3Properties of(long maxFileBytes, int maxComplaintImages, String signingKey) {
        return new S3Properties(
                false,
                "",
                "",
                "us-east-1",
                "samosa-junction",
                "",
                "",
                true,
                Duration.ofMinutes(15),
                maxFileBytes,
                maxComplaintImages,
                "data/object-storage",
                "http://localhost:8080",
                signingKey
        );
    }
}
