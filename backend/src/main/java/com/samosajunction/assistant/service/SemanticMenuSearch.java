package com.samosajunction.assistant.service;

import com.samosajunction.assistant.config.AssistantProperties;
import com.samosajunction.assistant.openrouter.OpenRouterClient;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.dto.ProductSearchCriteria;
import com.samosajunction.product.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class SemanticMenuSearch {

    private final ProductService productService;
    private final OpenRouterClient openRouterClient;
    private final AssistantProperties properties;

    public SemanticMenuSearch(
            ProductService productService,
            OpenRouterClient openRouterClient,
            AssistantProperties properties
    ) {
        this.productService = productService;
        this.openRouterClient = openRouterClient;
        this.properties = properties;
    }

    public List<ProductResponse> search(String query, int limit) {
        int topK = Math.max(1, Math.min(limit, properties.semanticTopK()));
        List<ProductResponse> catalog = availableCatalog();
        if (catalog.isEmpty()) {
            return List.of();
        }
        if (!properties.openRouterEnabled()) {
            return keywordFallback(query, topK);
        }
        try {
            List<String> texts = new ArrayList<>(catalog.size() + 1);
            texts.add(query == null ? "" : query.trim());
            for (ProductResponse product : catalog) {
                texts.add(document(product));
            }
            List<float[]> vectors = openRouterClient.embed(texts);
            if (vectors.size() != texts.size()) {
                return keywordFallback(query, topK);
            }
            float[] queryVector = vectors.getFirst();
            record Scored(ProductResponse product, double score) {
            }
            List<Scored> scored = new ArrayList<>();
            for (int i = 0; i < catalog.size(); i++) {
                scored.add(new Scored(catalog.get(i), cosine(queryVector, vectors.get(i + 1))));
            }
            return scored.stream()
                    .sorted(Comparator.comparingDouble(Scored::score).reversed())
                    .limit(topK)
                    .filter(row -> row.score() > 0.15)
                    .map(Scored::product)
                    .toList();
        } catch (RuntimeException ex) {
            return keywordFallback(query, topK);
        }
    }

    public List<ProductResponse> availableCatalog() {
        return productService.search(
                new ProductSearchCriteria(null, null, null, true, null),
                PageRequest.of(0, 100)
        ).getContent();
    }

    private List<ProductResponse> keywordFallback(String query, int topK) {
        String needle = query == null ? "" : query.trim();
        var page = productService.search(
                new ProductSearchCriteria(needle.isBlank() ? null : needle, null, null, true, null),
                PageRequest.of(0, topK)
        );
        if (!page.isEmpty()) {
            return page.getContent();
        }
        String loose = needle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").trim();
        return availableCatalog().stream()
                .filter(product -> score(product, loose) > 0)
                .sorted(Comparator.comparingInt((ProductResponse product) -> score(product, loose)).reversed())
                .limit(topK)
                .toList();
    }

    private static int score(ProductResponse product, String needle) {
        if (needle.isBlank()) {
            return 0;
        }
        String blob = (product.name() + " " + product.description() + " " + String.join(" ", product.ingredients()))
                .toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String token : needle.split("\\s+")) {
            if (token.length() >= 3 && blob.contains(token)) {
                hits += 10;
            }
        }
        if (blob.contains(needle)) {
            hits += 50;
        }
        return hits;
    }

    private static String document(ProductResponse product) {
        return product.name()
                + ". " + product.description()
                + ". Category " + product.category()
                + ". Ingredients " + String.join(", ", product.ingredients())
                + ". Dietary " + String.join(", ", product.dietaryTags())
                + ". Spice " + product.spiceLevel()
                + ". Protein " + product.protein() + "g. Calories " + product.calories()
                + ". Price INR " + product.price();
    }

    private static double cosine(float[] a, float[] b) {
        if (a.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
