package com.samosajunction.web;

import com.samosajunction.auth.security.JwtService;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.service.ProductImageService;
import com.samosajunction.product.service.ProductService;
import com.samosajunction.testsupport.SecureWebMvc;
import com.samosajunction.testsupport.WithSamosaUser;
import com.samosajunction.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.samosajunction.product.controller.ProductController.class)
@SecureWebMvc
class ProductCatalogWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductImageService productImageService;

    @MockitoBean
    private com.samosajunction.staff.service.StaffAuditService staffAuditService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void catalogIsPublic() throws Exception {
        when(productService.search(any(), any())).thenReturn(new PageImpl<>(List.of(sampleProduct())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Paneer Samosa"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verify(productService, never()).create(any());
    }

    @Test
    @WithSamosaUser
    void customerCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verify(productService, never()).create(any());
    }

    @Test
    @WithSamosaUser(role = "STAFF")
    void staffCanCreateProduct() throws Exception {
        when(productService.create(any())).thenReturn(sampleProduct());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paneer Samosa"));
    }

    private static ProductResponse sampleProduct() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new ProductResponse(
                UUID.fromString("11111111-1111-4111-8111-111111111112"),
                "Paneer Samosa",
                "Filled pastry",
                "PROTEIN",
                new BigDecimal("40.00"),
                Set.of("paneer"),
                310,
                new BigDecimal("12.00"),
                SpiceLevel.MILD,
                Set.of("HIGH_PROTEIN"),
                true,
                8,
                now,
                now,
                null
        );
    }

    private static String createBody() {
        return """
                {
                  "name":"Paneer Samosa",
                  "description":"Filled pastry",
                  "category":"PROTEIN",
                  "price":40.00,
                  "ingredients":["paneer"],
                  "calories":310,
                  "protein":12.00,
                  "spiceLevel":"MILD",
                  "dietaryTags":["HIGH_PROTEIN"],
                  "available":true
                }
                """;
    }
}
