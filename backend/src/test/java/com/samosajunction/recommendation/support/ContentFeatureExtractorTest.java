package com.samosajunction.recommendation.support;

import com.samosajunction.product.entity.Category;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.entity.SpiceLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ContentFeatureExtractorTest {

    @Test
    void tokensUseCategoryIngredientsTagsAndBands() {
        var product = new Product(
                "Paneer Samosa",
                "Crumbled paneer",
                new Category("PROTEIN", "High Protein"),
                4000,
                310,
                new BigDecimal("12.00"),
                SpiceLevel.MILD,
                true,
                Set.of("Paneer", "onion"),
                Set.of("vegetarian", "HIGH_PROTEIN")
        );

        assertThat(ContentFeatureExtractor.tokens(product)).containsExactlyInAnyOrder(
                "cat:PROTEIN",
                "spice:MILD",
                "cal:HIGH",
                "pro:HIGH",
                "ing:paneer",
                "ing:onion",
                "tag:VEGETARIAN",
                "tag:HIGH_PROTEIN"
        );
    }

    @Test
    void calorieAndProteinBandsSplitTheSeedCatalog() {
        assertThat(ContentFeatureExtractor.calorieBand(180)).isEqualTo("LOW");
        assertThat(ContentFeatureExtractor.calorieBand(260)).isEqualTo("MED");
        assertThat(ContentFeatureExtractor.calorieBand(310)).isEqualTo("HIGH");
        assertThat(ContentFeatureExtractor.proteinBand(new BigDecimal("6.00"))).isEqualTo("LOW");
        assertThat(ContentFeatureExtractor.proteinBand(new BigDecimal("8.00"))).isEqualTo("MED");
        assertThat(ContentFeatureExtractor.proteinBand(new BigDecimal("12.00"))).isEqualTo("HIGH");
    }
}
