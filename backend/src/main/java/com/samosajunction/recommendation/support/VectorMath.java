package com.samosajunction.recommendation.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VectorMath {

    private VectorMath() {
    }

    public static void addScaled(Map<String, Double> target, Map<String, Double> source, double scale) {
        source.forEach((term, value) -> target.merge(term, value * scale, Double::sum));
    }

    public static double cosine(Map<String, Double> left, Map<String, Double> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        double dot = 0;
        for (Map.Entry<String, Double> entry : left.entrySet()) {
            Double other = right.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        double denom = norm(left) * norm(right);
        if (denom == 0) {
            return 0;
        }
        return dot / denom;
    }

    public static List<String> overlappingTerms(Map<String, Double> profile, Map<String, Double> product, int limit) {
        record Scored(String term, double weight) {
        }
        List<Scored> scored = new ArrayList<>();
        product.forEach((term, weight) -> {
            if (profile.containsKey(term)) {
                scored.add(new Scored(term, Math.min(weight, profile.get(term))));
            }
        });
        scored.sort((a, b) -> Double.compare(b.weight, a.weight));
        return scored.stream().limit(limit).map(Scored::term).toList();
    }

    private static double norm(Map<String, Double> vector) {
        double sum = 0;
        for (double value : vector.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}
