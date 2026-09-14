package com.samosajunction.product.repository;

import com.samosajunction.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    Optional<ProductImage> findByProductId(UUID productId);

    List<ProductImage> findByProductIdIn(Collection<UUID> productIds);
}
