package com.samosajunction.recommendation.support;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TfidfModel {

    private final Map<String, Double> idf;

    private TfidfModel(Map<String, Double> idf) {
        this.idf = idf;
    }

    public static TfidfModel fit(List<List<String>> documents) {
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> document : documents) {
            document.stream().distinct().forEach(term ->
                    documentFrequency.merge(term, 1, Integer::sum)
            );
        }
        int n = documents.size();
        Map<String, Double> idf = new HashMap<>();
        documentFrequency.forEach((term, df) ->
                idf.put(term, Math.log((1.0 + n) / (1.0 + df)) + 1.0)
        );
        return new TfidfModel(Map.copyOf(idf));
    }

    public Map<String, Double> vector(List<String> tokens) {
        if (tokens.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        double length = tokens.size();
        Map<String, Double> vector = new LinkedHashMap<>();
        counts.forEach((term, count) -> {
            double weight = (count / length) * idf.getOrDefault(term, 1.0);
            if (weight > 0) {
                vector.put(term, weight);
            }
        });
        return vector;
    }
}
