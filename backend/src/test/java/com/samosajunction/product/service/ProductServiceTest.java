package com.samosajunction.product.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.product.dto.ProductRequest;
import com.samosajunction.product.entity.Category;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.repository.CategoryRepository;
import com.samosajunction.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private com.samosajunction.inventory.repository.InventoryRepository inventoryRepository;

    @Mock
    private ProductImageService productImageService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
                productRepository,
                categoryRepository,
                inventoryService,
                inventoryRepository,
                productImageService
        );
    }

    @Test
    void createPersistsPaiseAndNormalizesTags() {
        var category = new Category("PROTEIN", "High Protein");
        when(productRepository.existsByNameIgnoreCase("Paneer Samosa")).thenReturn(false);
        when(categoryRepository.findByCodeIgnoreCase("PROTEIN")).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
            return product;
        });

        var response = productService.create(request("Paneer Samosa", "PROTEIN", "40.00"));

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(saved.capture());
        verify(inventoryService).createForNewProduct(saved.getValue().getId());
        assertThat(saved.getValue().getPricePaise()).isEqualTo(4000);
        assertThat(saved.getValue().getIngredients()).containsExactlyInAnyOrder("paneer");
        assertThat(saved.getValue().getDietaryTags()).containsExactlyInAnyOrder("HIGH_PROTEIN");
        assertThat(response.price()).isEqualByComparingTo("40.00");
        assertThat(response.category()).isEqualTo("PROTEIN");
    }

    @Test
    void createRejectsDuplicateName() {
        when(productRepository.existsByNameIgnoreCase("Paneer Samosa")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request("Paneer Samosa", "PROTEIN", "40.00")))
                .isInstanceOf(ResourceConflictException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownCategory() {
        when(productRepository.existsByNameIgnoreCase("Paneer Samosa")).thenReturn(false);
        when(categoryRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request("Paneer Samosa", "NOPE", "40.00")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unknown category");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(productRepository.findDetailedById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchRejectsUnknownSortField() {
        var pageable = PageRequest.of(0, 20, Sort.by("hacked"));

        assertThatThrownBy(() -> productService.search(new com.samosajunction.product.dto.ProductSearchCriteria(
                null, null, null, null, null
        ), pageable)).isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    private static ProductRequest request(String name, String category, String price) {
        return new ProductRequest(
                name,
                "A filled pastry",
                category,
                new BigDecimal(price),
                Set.of("Paneer"),
                300,
                new BigDecimal("12.00"),
                SpiceLevel.MILD,
                Set.of("high_protein"),
                true
        );
    }
}
