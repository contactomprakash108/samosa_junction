package com.samosajunction.web;

import com.samosajunction.auth.security.JwtService;
import com.samosajunction.recommendation.controller.RecommendationController;
import com.samosajunction.recommendation.dto.RecommendationResponse;
import com.samosajunction.recommendation.service.RecommendationService;
import com.samosajunction.testsupport.SecureWebMvc;
import com.samosajunction.testsupport.WithSamosaUser;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RecommendationController.class)
@SecureWebMvc
class RecommendationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void recommendationsRequireJwt() throws Exception {
        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithSamosaUser
    void customerCanReadRecommendations() throws Exception {
        when(recommendationService.recommend(eq(UUID.fromString("11111111-1111-4111-8111-111111111111")), isNull()))
                .thenReturn(new RecommendationResponse(true, List.of(), List.of()));

        mockMvc.perform(get("/api/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coldStart").value(true));
    }
}
