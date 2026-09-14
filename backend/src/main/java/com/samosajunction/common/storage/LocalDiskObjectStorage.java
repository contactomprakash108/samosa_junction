package com.samosajunction.common.storage;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

public class LocalDiskObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalDiskObjectStorage.class);

    private final Path root;
    private final LocalObjectUrlSigner signer;
    private final Duration presignTtl;

    public LocalDiskObjectStorage(S3Properties properties, LocalObjectUrlSigner signer) {
        this.root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
        this.signer = signer;
        this.presignTtl = properties.presignTtl();
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            log.info("Stored object locally key={} bytes={} type={}", key, content.length, contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not write local object " + key, ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not delete local object " + key, ex);
        }
    }

    @Override
    public URI presignGet(String key, Duration ttl) {
        Duration effective = ttl == null ? presignTtl : ttl;
        return signer.sign(key, Instant.now().plus(effective));
    }

    public byte[] read(String key) {
        try {
            Path target = resolve(key);
            if (!Files.exists(target)) {
                throw new ResourceNotFoundException("Object not found");
            }
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read local object " + key, ex);
        }
    }

    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new InvalidRequestException("Invalid object key");
        }
        return target;
    }
}
