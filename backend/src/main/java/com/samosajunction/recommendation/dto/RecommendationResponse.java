package com.samosajunction.recommendation.dto;

import java.util.List;

public record RecommendationResponse(
        boolean coldStart,
        List<String> tasteTokens,
        List<RecommendationItemResponse> items
) {
}
