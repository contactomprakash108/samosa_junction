package com.samosajunction.complaint.dto;

import com.samosajunction.complaint.entity.ComplaintImage;

import java.time.Instant;
import java.util.UUID;

public record ComplaintImageResponse(
        UUID id,
        String fileName,
        String contentType,
        int fileSize,
        String url,
        Instant uploadedAt
) {
    public static ComplaintImageResponse from(ComplaintImage image, String url) {
        return new ComplaintImageResponse(
                image.getId(),
                image.getFileName(),
                image.getContentType(),
                image.getFileSize(),
                url,
                image.getUploadedAt()
        );
    }
}
