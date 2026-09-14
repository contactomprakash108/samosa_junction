package com.samosajunction.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(name = "s3_object_key", nullable = false, unique = true, length = 512)
    private String s3ObjectKey;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private int fileSize;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected ProductImage() {
    }

    public ProductImage(UUID productId, String s3ObjectKey, String fileName, String contentType, int fileSize) {
        this.productId = productId;
        this.s3ObjectKey = s3ObjectKey;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = Instant.now();
    }

    public void replace(String s3ObjectKey, String fileName, String contentType, int fileSize) {
        this.s3ObjectKey = s3ObjectKey;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public int getFileSize() {
        return fileSize;
    }
}
