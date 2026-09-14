package com.samosajunction.recommendation.dto;

import com.samosajunction.product.dto.ProductResponse;

public record RecommendationItemResponse(
        ProductResponse product,
        double score,
        String reason
) {
}
