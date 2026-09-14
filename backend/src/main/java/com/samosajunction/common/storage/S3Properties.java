package com.samosajunction.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "samosa.s3")
public record S3Properties(
        boolean enabled,
        String endpoint,
        String presignEndpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        boolean pathStyle,
        Duration presignTtl,
        long maxFileBytes,
        int maxComplaintImages,
        String localRoot,
        String publicBaseUrl,
        String signingKey
) {
    public String effectivePresignEndpoint() {
        if (presignEndpoint != null && !presignEndpoint.isBlank()) {
            return presignEndpoint;
        }
        return endpoint;
    }
}
