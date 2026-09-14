package com.samosajunction.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class ObjectStorageConfig {

    @Bean
    public LocalObjectUrlSigner localObjectUrlSigner(S3Properties properties) {
        return new LocalObjectUrlSigner(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "samosa.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
    public LocalDiskObjectStorage localDiskObjectStorage(S3Properties properties, LocalObjectUrlSigner signer) {
        return new LocalDiskObjectStorage(properties, signer);
    }

    @Bean
    @ConditionalOnProperty(prefix = "samosa.s3", name = "enabled", havingValue = "true")
    public S3Client s3Client(S3Properties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyle())
                        .build())
                .credentialsProvider(credentials(properties));
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "samosa.s3", name = "enabled", havingValue = "true")
    public S3Presigner s3Presigner(S3Properties properties) {
        var builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyle())
                        .build())
                .credentialsProvider(credentials(properties));
        String presignEndpoint = properties.effectivePresignEndpoint();
        if (presignEndpoint != null && !presignEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(presignEndpoint));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "samosa.s3", name = "enabled", havingValue = "true")
    public ObjectStorage s3ObjectStorage(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
        return new S3ObjectStorage(s3Client, s3Presigner, properties);
    }

    private static software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentials(S3Properties properties) {
        if (properties.accessKey() == null || properties.accessKey().isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
        );
    }
}
