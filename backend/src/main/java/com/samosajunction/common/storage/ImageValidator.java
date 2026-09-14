package com.samosajunction.common.storage;

import com.samosajunction.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Component
public class ImageValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final S3Properties properties;

    public ImageValidator(S3Properties properties) {
        this.properties = properties;
    }

    public ValidatedImage requireImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("An image file is required");
        }
        if (file.getSize() > properties.maxFileBytes()) {
            throw new InvalidRequestException(
                    "Image cannot exceed %d bytes".formatted(properties.maxFileBytes())
            );
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidRequestException("Only JPEG, PNG, and WebP images are allowed");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InvalidRequestException("Could not read the uploaded file");
        }
        if (!matchesMagic(bytes, contentType)) {
            throw new InvalidRequestException("File content does not match the declared image type");
        }
        return new ValidatedImage(bytes, contentType, sanitizeFileName(file.getOriginalFilename(), contentType));
    }

    static String sanitizeFileName(String original, String contentType) {
        String fallback = switch (contentType) {
            case "image/png" -> "image.png";
            case "image/webp" -> "image.webp";
            default -> "image.jpg";
        };
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank() || name.startsWith(".")) {
            return fallback;
        }
        return name.length() > 180 ? name.substring(name.length() - 180) : name;
    }

    private static boolean matchesMagic(byte[] bytes, String contentType) {
        if (bytes.length < 12) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
            case "image/png" -> bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
            case "image/webp" -> bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    public record ValidatedImage(byte[] content, String contentType, String fileName) {
    }
}
