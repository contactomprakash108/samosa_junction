package com.samosajunction.complaint.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.common.storage.ImageValidator;
import com.samosajunction.common.storage.ObjectStorage;
import com.samosajunction.common.storage.S3Properties;
import com.samosajunction.complaint.dto.ComplaintImageResponse;
import com.samosajunction.complaint.entity.Complaint;
import com.samosajunction.complaint.entity.ComplaintImage;
import com.samosajunction.complaint.repository.ComplaintImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ComplaintImageService {

    private final ComplaintService complaintService;
    private final ComplaintImageRepository complaintImageRepository;
    private final ObjectStorage objectStorage;
    private final ImageValidator imageValidator;
    private final S3Properties properties;

    public ComplaintImageService(
            ComplaintService complaintService,
            ComplaintImageRepository complaintImageRepository,
            ObjectStorage objectStorage,
            ImageValidator imageValidator,
            S3Properties properties
    ) {
        this.complaintService = complaintService;
        this.complaintImageRepository = complaintImageRepository;
        this.objectStorage = objectStorage;
        this.imageValidator = imageValidator;
        this.properties = properties;
    }

    @Transactional
    public ComplaintImageResponse upload(UUID actorId, UUID complaintId, boolean privileged, MultipartFile file) {
        Complaint complaint = complaintService.requireVisible(actorId, complaintId, privileged);
        if (complaintImageRepository.countByComplaintId(complaint.getId()) >= properties.maxComplaintImages()) {
            throw new InvalidRequestException(
                    "A complaint can have at most %d images".formatted(properties.maxComplaintImages())
            );
        }
        var validated = imageValidator.requireImage(file);
        UUID imageId = UUID.randomUUID();
        String key = "complaints/%s/%s-%s".formatted(complaint.getId(), imageId, validated.fileName());
        objectStorage.put(key, validated.content(), validated.contentType());
        try {
            ComplaintImage saved = complaintImageRepository.saveAndFlush(new ComplaintImage(
                    complaint.getId(),
                    key,
                    validated.fileName(),
                    validated.contentType(),
                    validated.content().length
            ));
            return toResponse(saved);
        } catch (RuntimeException ex) {
            objectStorage.delete(key);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ComplaintImageResponse> list(UUID actorId, UUID complaintId, boolean privileged) {
        complaintService.requireVisible(actorId, complaintId, privileged);
        return complaintImageRepository.findByComplaintIdOrderByUploadedAtAsc(complaintId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID actorId, UUID complaintId, UUID imageId, boolean privileged) {
        complaintService.requireVisible(actorId, complaintId, privileged);
        ComplaintImage image = complaintImageRepository.findByIdAndComplaintId(imageId, complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));
        complaintImageRepository.delete(image);
        objectStorage.delete(image.getS3ObjectKey());
    }

    public List<ComplaintImageResponse> listForComplaint(UUID complaintId) {
        return complaintImageRepository.findByComplaintIdOrderByUploadedAtAsc(complaintId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ComplaintImageResponse toResponse(ComplaintImage image) {
        return ComplaintImageResponse.from(
                image,
                objectStorage.presignGet(image.getS3ObjectKey(), properties.presignTtl()).toString()
        );
    }
}
