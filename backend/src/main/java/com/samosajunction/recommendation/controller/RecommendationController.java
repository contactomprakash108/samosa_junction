package com.samosajunction.recommendation.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.recommendation.dto.RecommendationResponse;
import com.samosajunction.recommendation.service.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public RecommendationResponse list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer limit
    ) {
        return recommendationService.recommend(principal.getId(), limit);
    }
}
