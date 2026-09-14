package com.samosajunction.product.service;

import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.common.storage.ImageValidator;
import com.samosajunction.common.storage.ObjectStorage;
import com.samosajunction.common.storage.S3Properties;
import com.samosajunction.product.entity.ProductImage;
import com.samosajunction.product.repository.ProductImageRepository;
import com.samosajunction.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ObjectStorage objectStorage;
    private final ImageValidator imageValidator;
    private final S3Properties properties;

    public ProductImageService(
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ObjectStorage objectStorage,
            ImageValidator imageValidator,
            S3Properties properties
    ) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.objectStorage = objectStorage;
        this.imageValidator = imageValidator;
        this.properties = properties;
    }

    @Transactional
    public String upload(UUID productId, MultipartFile file) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        var validated = imageValidator.requireImage(file);
        String key = "products/%s/%s".formatted(productId, validated.fileName());
        objectStorage.put(key, validated.content(), validated.contentType());
        var existing = productImageRepository.findByProductId(productId);
        String previous = existing.map(ProductImage::getS3ObjectKey).orElse(null);
        try {
            if (existing.isPresent()) {
                existing.get().replace(key, validated.fileName(), validated.contentType(), validated.content().length);
            } else {
                productImageRepository.save(new ProductImage(
                        productId,
                        key,
                        validated.fileName(),
                        validated.contentType(),
                        validated.content().length
                ));
            }
        } catch (RuntimeException ex) {
            objectStorage.delete(key);
            throw ex;
        }
        if (previous != null && !previous.equals(key)) {
            objectStorage.delete(previous);
        }
        return objectStorage.presignGet(key, properties.presignTtl()).toString();
    }

    @Transactional
    public void delete(UUID productId) {
        ProductImage image = productImageRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));
        productImageRepository.delete(image);
        objectStorage.delete(image.getS3ObjectKey());
    }

    @Transactional
    public void deleteIfPresent(UUID productId) {
        productImageRepository.findByProductId(productId).ifPresent(image -> {
            productImageRepository.delete(image);
            objectStorage.delete(image.getS3ObjectKey());
        });
    }

    public Optional<String> presignedUrl(UUID productId) {
        return productImageRepository.findByProductId(productId)
                .map(image -> objectStorage.presignGet(image.getS3ObjectKey(), properties.presignTtl()).toString());
    }

    public Map<UUID, String> presignedUrls(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return productImageRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        ProductImage::getProductId,
                        image -> objectStorage.presignGet(image.getS3ObjectKey(), properties.presignTtl()).toString()
                ));
    }
}
