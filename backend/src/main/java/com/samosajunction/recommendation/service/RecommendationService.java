package com.samosajunction.recommendation.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.order.entity.Order;
import com.samosajunction.order.entity.OrderItem;
import com.samosajunction.order.entity.OrderStatus;
import com.samosajunction.order.repository.OrderRepository;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.product.service.ProductImageService;
import com.samosajunction.recommendation.dto.RecommendationItemResponse;
import com.samosajunction.recommendation.dto.RecommendationResponse;
import com.samosajunction.recommendation.support.ContentFeatureExtractor;
import com.samosajunction.recommendation.support.TfidfModel;
import com.samosajunction.recommendation.support.VectorMath;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    static final Set<OrderStatus> PAID_STATUSES = EnumSet.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.OUT_FOR_DELIVERY,
            OrderStatus.DELIVERED
    );

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;
    private static final int TASTE_TOKEN_LIMIT = 5;
    private static final int REASON_TOKEN_LIMIT = 3;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductImageService productImageService;

    public RecommendationService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            ProductImageService productImageService
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.productImageService = productImageService;
    }

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(UUID userId, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            throw new InvalidRequestException("limit must be between 1 and 10");
        }

        List<Product> catalog = productRepository.findAllWithCategory();
        List<Product> available = catalog.stream().filter(Product::isAvailable).toList();
        Map<UUID, String> urls = productImageService.presignedUrls(
                available.stream().map(Product::getId).toList()
        );
        if (catalog.isEmpty()) {
            return new RecommendationResponse(true, List.of(), List.of());
        }

        Map<UUID, Product> byId = catalog.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<UUID, Integer> orderedQty = paidQuantities(userId);
        TfidfModel model = TfidfModel.fit(catalog.stream().map(ContentFeatureExtractor::tokens).toList());
        Map<String, Double> profile = tasteProfile(model, orderedQty, byId);

        if (profile.isEmpty()) {
            return coldStart(available, urls, size);
        }
        return personalized(model, available, orderedQty.keySet(), profile, urls, size);
    }

    private Map<UUID, Integer> paidQuantities(UUID userId) {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (Order order : orderRepository.findWithItemsByUserIdAndStatusIn(userId, PAID_STATUSES)) {
            for (OrderItem item : order.getItems()) {
                quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }
        return quantities;
    }

    private static Map<String, Double> tasteProfile(
            TfidfModel model,
            Map<UUID, Integer> orderedQty,
            Map<UUID, Product> byId
    ) {
        Map<String, Double> profile = new HashMap<>();
        orderedQty.forEach((productId, quantity) -> {
            Product product = byId.get(productId);
            if (product != null) {
                VectorMath.addScaled(profile, model.vector(ContentFeatureExtractor.tokens(product)), quantity);
            }
        });
        return profile;
    }

    private RecommendationResponse personalized(
            TfidfModel model,
            List<Product> available,
            Set<UUID> orderedIds,
            Map<String, Double> profile,
            Map<UUID, String> urls,
            int limit
    ) {
        List<Product> candidates = available.stream()
                .filter(product -> !orderedIds.contains(product.getId()))
                .toList();
        if (candidates.size() < limit) {
            List<Product> merged = new ArrayList<>(candidates);
            available.stream()
                    .filter(product -> orderedIds.contains(product.getId()))
                    .forEach(merged::add);
            candidates = merged;
        }

        List<String> tasteTokens = topTerms(profile, TASTE_TOKEN_LIMIT);
        List<RecommendationItemResponse> items = candidates.stream()
                .map(product -> {
                    Map<String, Double> vector = model.vector(ContentFeatureExtractor.tokens(product));
                    double score = round(VectorMath.cosine(profile, vector));
                    List<String> overlap = VectorMath.overlappingTerms(profile, vector, REASON_TOKEN_LIMIT);
                    return new ScoredProduct(product, score, overlap);
                })
                .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed()
                        .thenComparing(scored -> scored.product().getName()))
                .limit(limit)
                .map(scored -> new RecommendationItemResponse(
                        ProductResponse.from(scored.product(), urls.get(scored.product().getId())),
                        scored.score(),
                        personalizedReason(scored.overlap())
                ))
                .toList();
        return new RecommendationResponse(false, tasteTokens, items);
    }

    private RecommendationResponse coldStart(List<Product> available, Map<UUID, String> urls, int limit) {
        List<RecommendationItemResponse> items = available.stream()
                .sorted(Comparator.comparing(Product::getProteinGrams).reversed()
                        .thenComparing(Product::getName))
                .limit(limit)
                .map(product -> new RecommendationItemResponse(
                        ProductResponse.from(product, urls.get(product.getId())),
                        0.0,
                        "No paid orders yet; showing available catalog."
                ))
                .toList();
        return new RecommendationResponse(true, List.of(), items);
    }

    private static String personalizedReason(List<String> overlap) {
        if (overlap.isEmpty()) {
            return "Limited overlap with your paid orders.";
        }
        return "Matches your taste: " + String.join(", ", overlap);
    }

    private static List<String> topTerms(Map<String, Double> profile, int limit) {
        return profile.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private record ScoredProduct(Product product, double score, List<String> overlap) {
    }
}
