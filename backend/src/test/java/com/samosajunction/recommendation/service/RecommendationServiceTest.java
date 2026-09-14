package com.samosajunction.recommendation.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.product.entity.Category;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.product.service.ProductImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID CLASSIC_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID PANEER_ID = UUID.fromString("11111111-1111-4111-8111-111111111112");
    private static final UUID HIGH_PROTEIN_ID = UUID.fromString("11111111-1111-4111-8111-111111111115");
    private static final UUID CHOLE_ID = UUID.fromString("11111111-1111-4111-8111-111111111117");
    private static final UUID HIDDEN_ID = UUID.fromString("11111111-1111-4111-8111-111111111199");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductImageService productImageService;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(productRepository, orderRepository, productImageService);
    }

    @Test
    void paneerOrdersRankHighProteinAboveClassicAndChole() {
        when(productRepository.findAllWithCategory()).thenReturn(seedCatalog());
        when(orderRepository.findWithItemsByUserIdAndStatusIn(eq(USER_ID), eq(RecommendationService.PAID_STATUSES)))
                .thenReturn(List.of(paidOrder(PANEER_ID, 2)));
        when(productImageService.presignedUrls(any())).thenReturn(Map.of());

        var response = recommendationService.recommend(USER_ID, 5);

        assertThat(response.coldStart()).isFalse();
        assertThat(response.tasteTokens()).contains("ing:paneer");
        assertThat(response.items()).extracting(item -> item.product().id())
                .doesNotContain(PANEER_ID, HIDDEN_ID);
        assertThat(indexOf(response, HIGH_PROTEIN_ID))
                .isLessThan(indexOf(response, CLASSIC_ID))
                .isLessThan(indexOf(response, CHOLE_ID));
        assertThat(scoreOf(response, HIGH_PROTEIN_ID)).isGreaterThan(scoreOf(response, CLASSIC_ID));
        assertThat(response.items().getFirst().reason()).contains("Matches your taste");
    }

    @Test
    void createdOrdersDoNotBuildATasteProfile() {
        when(productRepository.findAllWithCategory()).thenReturn(seedCatalog());
        when(orderRepository.findWithItemsByUserIdAndStatusIn(eq(USER_ID), eq(RecommendationService.PAID_STATUSES)))
                .thenReturn(List.of());
        when(productImageService.presignedUrls(any())).thenReturn(Map.of());

        var response = recommendationService.recommend(USER_ID, 3);

        assertThat(response.coldStart()).isTrue();
        assertThat(response.tasteTokens()).isEmpty();
        assertThat(response.items()).extracting(item -> item.product().id())
                .containsExactly(HIGH_PROTEIN_ID, PANEER_ID, CHOLE_ID);
        assertThat(response.items()).allMatch(item -> item.score() == 0.0);
        assertThat(response.items().getFirst().reason()).contains("No paid orders yet");
    }

    @Test
    void unavailableProductsAreExcluded() {
        when(productRepository.findAllWithCategory()).thenReturn(seedCatalog());
        when(orderRepository.findWithItemsByUserIdAndStatusIn(eq(USER_ID), eq(RecommendationService.PAID_STATUSES)))
                .thenReturn(List.of());
        when(productImageService.presignedUrls(any())).thenReturn(Map.of());

        var response = recommendationService.recommend(USER_ID, 10);

        assertThat(response.items()).extracting(item -> item.product().id()).doesNotContain(HIDDEN_ID);
        assertThat(response.items()).allMatch(item -> item.product().available());
    }

    @Test
    void defaultLimitIsFive() {
        when(productRepository.findAllWithCategory()).thenReturn(seedCatalog());
        when(orderRepository.findWithItemsByUserIdAndStatusIn(eq(USER_ID), eq(RecommendationService.PAID_STATUSES)))
                .thenReturn(List.of());
        when(productImageService.presignedUrls(any())).thenReturn(Map.of());

        assertThat(recommendationService.recommend(USER_ID, null).items()).hasSize(5);
    }

    @Test
    void rejectsLimitOutsideOneToTen() {
        assertThatThrownBy(() -> recommendationService.recommend(USER_ID, 0))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> recommendationService.recommend(USER_ID, 11))
                .isInstanceOf(InvalidRequestException.class);
    }

    private static int indexOf(
            com.samosajunction.recommendation.dto.RecommendationResponse response,
            UUID productId
    ) {
        for (int i = 0; i < response.items().size(); i++) {
            if (response.items().get(i).product().id().equals(productId)) {
                return i;
            }
        }
        throw new AssertionError("missing product " + productId);
    }

    private static double scoreOf(
            com.samosajunction.recommendation.dto.RecommendationResponse response,
            UUID productId
    ) {
        return response.items().stream()
                .filter(item -> item.product().id().equals(productId))
                .findFirst()
                .orElseThrow()
                .score();
    }

    private static Order paidOrder(UUID productId, int quantity) {
        var order = new Order(USER_ID, "Ada", "1 Street", "Pune", "MH", "411001", "key-1");
        order.addItem(new OrderItem(productId, "Paneer Samosa", 4000, quantity));
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        return order;
    }

    private static List<Product> seedCatalog() {
        var classic = new Category("CLASSIC", "Classic");
        var protein = new Category("PROTEIN", "High Protein");
        var baked = new Category("BAKED", "Baked");
        var healthy = new Category("HEALTHY", "Healthy");
        return List.of(
                product(CLASSIC_ID, "Classic Samosa", classic, 260, "6.00", SpiceLevel.MEDIUM, true,
                        Set.of("potato", "peas", "wheat flour"), Set.of("VEGETARIAN")),
                product(PANEER_ID, "Paneer Samosa", protein, 310, "12.00", SpiceLevel.MILD, true,
                        Set.of("paneer", "onion", "wheat flour"), Set.of("VEGETARIAN", "HIGH_PROTEIN")),
                product(UUID.fromString("11111111-1111-4111-8111-111111111113"), "Corn Samosa", classic,
                        240, "7.00", SpiceLevel.MILD, true, Set.of("corn", "capsicum"), Set.of("VEGETARIAN")),
                product(UUID.fromString("11111111-1111-4111-8111-111111111114"), "Baked Samosa", baked,
                        180, "6.00", SpiceLevel.MILD, true, Set.of("potato", "wheat flour"),
                        Set.of("VEGETARIAN", "BAKED")),
                product(HIGH_PROTEIN_ID, "High Protein Samosa", protein, 290, "18.00", SpiceLevel.MEDIUM, true,
                        Set.of("paneer", "lentils"), Set.of("VEGETARIAN", "HIGH_PROTEIN")),
                product(UUID.fromString("11111111-1111-4111-8111-111111111116"), "Millet Samosa", healthy,
                        210, "8.00", SpiceLevel.MEDIUM, true, Set.of("millet flour", "mixed vegetables"),
                        Set.of("VEGETARIAN", "MILLET")),
                product(CHOLE_ID, "Chole Samosa", classic, 330, "11.00", SpiceLevel.HOT, true,
                        Set.of("chickpeas", "onion"), Set.of("VEGETARIAN", "HIGH_PROTEIN")),
                product(HIDDEN_ID, "Hidden Samosa", classic, 400, "20.00", SpiceLevel.MILD, false,
                        Set.of("paneer"), Set.of("HIGH_PROTEIN"))
        );
    }

    private static Product product(
            UUID id,
            String name,
            Category category,
            int calories,
            String protein,
            SpiceLevel spiceLevel,
            boolean available,
            Set<String> ingredients,
            Set<String> tags
    ) {
        var product = new Product(
                name,
                name,
                category,
                3000,
                calories,
                new BigDecimal(protein),
                spiceLevel,
                available,
                ingredients,
                tags
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
