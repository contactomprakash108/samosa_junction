package com.samosajunction.recommendation.support;

import com.samosajunction.product.entity.Product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ContentFeatureExtractor {

    private ContentFeatureExtractor() {
    }

    public static List<String> tokens(Product product) {
        List<String> tokens = new ArrayList<>();
        tokens.add("cat:" + product.getCategory().getCode().toUpperCase(Locale.ROOT));
        tokens.add("spice:" + product.getSpiceLevel().name());
        tokens.add("cal:" + calorieBand(product.getCalories()));
        tokens.add("pro:" + proteinBand(product.getProteinGrams()));
        for (String ingredient : product.getIngredients()) {
            tokens.add("ing:" + ingredient.toLowerCase(Locale.ROOT).trim());
        }
        for (String tag : product.getDietaryTags()) {
            tokens.add("tag:" + tag.toUpperCase(Locale.ROOT).trim());
        }
        return tokens;
    }

    static String calorieBand(int calories) {
        if (calories < 220) {
            return "LOW";
        }
        if (calories < 300) {
            return "MED";
        }
        return "HIGH";
    }

    static String proteinBand(BigDecimal proteinGrams) {
        double grams = proteinGrams.doubleValue();
        if (grams < 8) {
            return "LOW";
        }
        if (grams < 12) {
            return "MED";
        }
        return "HIGH";
    }
}
