package com.samosajunction.recommendation.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VectorMathTest {

    @Test
    void cosineOfAVectorWithItselfIsOne() {
        Map<String, Double> vector = Map.of("ing:paneer", 0.4, "tag:HIGH_PROTEIN", 0.6);
        assertThat(VectorMath.cosine(vector, vector)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void cosineIsZeroWhenThereIsNoOverlap() {
        assertThat(VectorMath.cosine(Map.of("ing:paneer", 1.0), Map.of("ing:potato", 1.0))).isZero();
        assertThat(VectorMath.cosine(Map.of(), Map.of("ing:paneer", 1.0))).isZero();
    }

    @Test
    void addScaledAccumulatesQuantity() {
        Map<String, Double> profile = new HashMap<>();
        VectorMath.addScaled(profile, Map.of("ing:paneer", 1.0), 2);
        VectorMath.addScaled(profile, Map.of("ing:paneer", 1.0, "tag:HIGH_PROTEIN", 0.5), 1);
        assertThat(profile.get("ing:paneer")).isEqualTo(3.0);
        assertThat(profile.get("tag:HIGH_PROTEIN")).isEqualTo(0.5);
    }

    @Test
    void overlappingTermsPreferSharedHighWeightKeys() {
        Map<String, Double> profile = Map.of("ing:paneer", 0.9, "tag:HIGH_PROTEIN", 0.2, "spice:HOT", 0.1);
        Map<String, Double> product = Map.of("ing:paneer", 0.8, "tag:HIGH_PROTEIN", 0.7, "ing:potato", 0.5);
        assertThat(VectorMath.overlappingTerms(profile, product, 2))
                .containsExactly("ing:paneer", "tag:HIGH_PROTEIN");
    }
}
