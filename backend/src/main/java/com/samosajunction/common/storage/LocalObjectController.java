package com.samosajunction.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "samosa.s3", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalObjectController {

    private final LocalDiskObjectStorage storage;
    private final LocalObjectUrlSigner signer;

    public LocalObjectController(LocalDiskObjectStorage storage, LocalObjectUrlSigner signer) {
        this.storage = storage;
        this.signer = signer;
    }

    @GetMapping("/api/objects")
    public ResponseEntity<byte[]> get(
            @RequestParam String key,
            @RequestParam long exp,
            @RequestParam String sig
    ) {
        signer.verify(key, exp, sig);
        byte[] body = storage.read(key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                .contentType(mediaType(key))
                .body(body);
    }

    private static MediaType mediaType(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
